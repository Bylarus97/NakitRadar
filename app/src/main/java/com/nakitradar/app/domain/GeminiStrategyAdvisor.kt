package com.nakitradar.app.domain

import com.nakitradar.app.data.AppRegistry
import com.nakitradar.app.data.WebCampaignScraper
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.CampaignSource
import com.nakitradar.app.model.CashbackType
import com.nakitradar.app.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hem çevrimdışı akıllı Türkçe kural çıkarımı (NLP/Regex) hem de isteğe bağlı Google Gemini API
 * entegrasyonu ile ekran görüntüsü metni / kampanya koşullarını analiz edip stratejiye dönüştürür.
 */
class GeminiStrategyAdvisor {

    /**
     * Kullanıcının yapıştırdığı veya paylaştığı uygulama içi kampanya metnini yapılandırılmış
     * Campaign nesnesine dönüştürür ve anında Strateji Motoruna dahil eder.
     */
    fun parseCustomCampaignText(
        rawText: String,
        selectedApp: InstalledAppInfo
    ): Campaign {
        val reward = WebCampaignScraper.extractRewardAmount(rawText)
        val minSpend = WebCampaignScraper.extractMinSpendAmount(rawText, reward)
        val rate = WebCampaignScraper.extractPercentage(rawText)
        val category = WebCampaignScraper.guessCategory(rawText)

        val stepCountRegex = Regex("""(\d+)\s*(adet|farklı gün|kez|defa)""", RegexOption.IGNORE_CASE)
        val stepCount = stepCountRegex.find(rawText)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1

        val firstSentence = rawText.lineSequence().firstOrNull { it.isNotBlank() }?.take(95)
            ?: "${selectedApp.name} Özel Nakit İade Kampanyası"

        return Campaign(
            id = "ai_custom_${selectedApp.id}_${System.currentTimeMillis()}",
            appId = selectedApp.id,
            appName = selectedApp.name,
            title = firstSentence,
            description = rawText.trim(),
            whereToFindInApp = "${selectedApp.name} > ${selectedApp.hiddenCampaignPathHint}",
            category = category,
            cashbackType = CashbackType.DIRECT_CASHBACK,
            minSpendAmount = minSpend,
            requiredTransactionCount = stepCount,
            cashbackRatePercent = rate,
            flatCashbackAmount = reward,
            maxCashbackCap = if (stepCount > 1) reward * stepCount else reward,
            requiresPreJoin = rawText.contains("katıl", ignoreCase = true) || rawText.contains("sms", ignoreCase = true),
            canStackAsWalletTopUp = true,
            validUntil = "Kullanıcı Tarafından Eklendi",
            source = CampaignSource.AI_IMPORTED
        )
    }

    /**
     * Gemini API anahtarı girilmişse Gemini 2.5 Flash modeline bağlanarak, girilmemişse yerleşik
     * Akıllı Nakit İade Uzman Motoru ile kişiselleştirilmiş taktiksel rapor üretir.
     */
    suspend fun askAiStrategyAdvisor(
        userQuestion: String,
        installedApps: List<InstalledAppInfo>,
        campaigns: List<Campaign>,
        geminiApiKey: String
    ): String = withContext(Dispatchers.IO) {
        val activeApps = installedApps.filter { it.isSelectedByUser }
        val activeAppNames = activeApps.joinToString(", ") { it.name }

        if (geminiApiKey.isNotBlank()) {
            try {
                return@withContext callGeminiApi(userQuestion, activeAppNames, campaigns, geminiApiKey.trim())
            } catch (e: Exception) {
                // API hatası durumunda yerleşik uzman analiz motoruna geri dön
            }
        }

        buildLocalExpertAnalysis(userQuestion, activeApps, campaigns)
    }

    private fun callGeminiApi(
        question: String,
        activeAppNames: String,
        campaigns: List<Campaign>,
        apiKey: String
    ): String {
        val campaignSummary = campaigns.joinToString("\n") {
            "- [${it.appName}] ${it.title}: Min ${it.minSpendAmount.toInt()} TL, İade: ${it.maxCashbackCap.toInt()} TL (${it.whereToFindInApp})"
        }

        val systemPrompt = """
            Sen Türkiye'deki banka (BonusFlaş, Juzdan, World Mobil, Maximum, Bankkart), operatör (Turkcell) ve
            ön ödemeli cüzdan (Paycell, Nays, Papara, Tosla, Hadi) kampanyalarında uzman bir Nakit İade (Cashback) Stratejistisin.
            Kullanıcının telefonunda yüklü uygulamalar: $activeAppNames
            Mevcut aktif kampanyalar:
            $campaignSummary

            Kullanıcının sorusuna göre:
            1) Hangi uygulamada hangi gizli sekmeden 'Hemen Katıl' yapması gerektiğini,
            2) Turkcell + Paycell + Banka kartı kombosunu (stacking) nasıl kuracağını,
            3) Harcamayı nasıl bölerek maksimum TL nakit iade alacağını net rakamlarla adım adım açıkla.
        """.trimIndent()

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
            connectTimeout = 10000
            readTimeout = 15000
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", "$systemPrompt\n\nKullanıcı Sorusu: $question"))
                    )
                )
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }

        val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(responseText)
        return json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    private fun buildLocalExpertAnalysis(
        question: String,
        activeApps: List<InstalledAppInfo>,
        campaigns: List<Campaign>
    ): String {
        val activeIds = activeApps.map { it.id }.toSet()
        val activeCampaigns = campaigns.filter { it.appId in activeIds }
        val totalMaxPotential = activeCampaigns.sumOf { it.maxCashbackCap }.toInt()

        val hasTurkcell = "turkcell" in activeIds
        val hasPaycell = "paycell" in activeIds
        val hasNays = "nays" in activeIds

        return buildString {
            appendLine("📊 NAKİTRADAR AKILLI STRATEJİ RAPORU")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("• Cihazınızdaki Aktif Uygulama Sayısı: ${activeApps.size} (${activeApps.joinToString { it.shortBadge }})")
            appendLine("• Bu Ay Toplam Nakit İade Potansiyeliniz: $totalMaxPotential TL")
            appendLine()

            if (hasTurkcell && hasPaycell) {
                appendLine("⚡ 1. TURKCELL + PAYCELL ÇİFTE İADE ZİNCİRİ (ÖNCELİKLİ):")
                appendLine("   • Adım A: Paycell uygulamasını açın -> 'Fırsatlar > Fatura & Market' sekmesinden kampanyalara 'Hemen Katıl' deyin.")
                appendLine("   • Adım B: Turkcell uygulamasını açın -> 'Fatura' sekmesine girin, ödeme yöntemi olarak Paycell'i seçip faturanızı ödeyin.")
                appendLine("   • Kazanç: Hem Turkcell'den 150 TL hem Paycell'den 75 TL olmak üzere tek faturadan 225 TL nakit iade alırsınız!")
                appendLine()
            }

            appendLine("🔥 2. BANKA KARTI + ÖN ÖDEMELİ CÜZDAN KOMBOSU (STACKING):")
            appendLine("   • Harcamanızı doğrudan banka kartıyla yapmak yerine önce BonusFlaş / Juzdan / World Mobil içindeki 'Bana Özel / Senin İçin' sekmesinden E-Ticaret veya Market kampanyasına katılın.")
            if (hasPaycell || hasNays) {
                appendLine("   • Ardından ödemenizi Paycell Kart/QR veya Nays üzerinden gerçekleştirerek hem banka puanını hem de cüzdan nakit iadesini (%10 - 150 TL) aynı anda tetikleyin.")
            }
            appendLine()

            appendLine("✂️ 3. HARCAMA BÖLME (TAVAN AŞMA) TAKTİĞİ:")
            appendLine("   • Kampanyaların çoğu 1.000 TL - 1.500 TL harcamada 150-200 TL üst limite takılır.")
            appendLine("   • 2.500 TL ve üzeri alışverişlerde ödemeyi tek karttan çekmek yerine 1.000 TL'sini Paycell, 1.000 TL'sini Banka Kartı (Bonus/Juzdan), kalanını Nays ile ödeyerek iadenizi 2.5 katına çıkarın!")
            appendLine()
            appendLine("💡 Sorunuz ('$question') için en yüksek iadeli kombinasyonları 'Strateji Motoru' sekmesinden tutar girerek kuruşu kuruşuna simüle edebilirsiniz.")
        }
    }
}
