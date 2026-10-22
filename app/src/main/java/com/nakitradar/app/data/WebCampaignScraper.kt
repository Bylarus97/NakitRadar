package com.nakitradar.app.data

import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.CampaignSource
import com.nakitradar.app.model.CashbackType
import com.nakitradar.app.model.InstalledAppInfo
import com.nakitradar.app.model.SpendCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Cihazda yüklü olan uygulamaların (Paycell, Turkcell, Bankalar vb.) resmi web kampanya
 * sayfalarını tarayıp "nakit iade", "cashback", "TL puan", "hediye" içeren yeni kampanyaları çeker.
 */
class WebCampaignScraper {

    suspend fun fetchLiveCampaignsForApps(installedApps: List<InstalledAppInfo>): List<Campaign> =
        withContext(Dispatchers.IO) {
            val discovered = mutableListOf<Campaign>()

            for (app in installedApps.filter { it.isSelectedByUser }) {
                try {
                    val doc = Jsoup.connect(app.officialCampaignUrl)
                        .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36")
                        .timeout(6000)
                        .get()

                    val headlines = doc.select("h2, h3, h4, .campaign-title, .card-title, a[href*=kampanya]")
                        .map { it.text().trim() }
                        .filter { text ->
                            text.length in 20..140 &&
                                (text.contains("TL", ignoreCase = true) ||
                                    text.contains("iade", ignoreCase = true) ||
                                    text.contains("nakit", ignoreCase = true) ||
                                    text.contains("puan", ignoreCase = true) ||
                                    text.contains("chip", ignoreCase = true) ||
                                    text.contains("bonus", ignoreCase = true))
                        }
                        .distinct()
                        .take(3)

                    headlines.forEachIndexed { idx, title ->
                        val parsedReward = extractRewardAmount(title)
                        val parsedMinSpend = extractMinSpendAmount(title, parsedReward)
                        if (parsedReward > 0.0) {
                            discovered.add(
                                Campaign(
                                    id = "live_${app.id}_$idx",
                                    appId = app.id,
                                    appName = app.name,
                                    title = title,
                                    description = "${app.name} resmi kampanya sayfasından canlı tarama ile keşfedildi.",
                                    whereToFindInApp = app.hiddenCampaignPathHint,
                                    category = guessCategory(title),
                                    cashbackType = CashbackType.DIRECT_CASHBACK,
                                    minSpendAmount = parsedMinSpend,
                                    requiredTransactionCount = 1,
                                    cashbackRatePercent = extractPercentage(title),
                                    flatCashbackAmount = parsedReward,
                                    maxCashbackCap = parsedReward,
                                    requiresPreJoin = true,
                                    validUntil = "Güncel Kampanya",
                                    source = CampaignSource.OFFICIAL_WEB
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Çevrimdışı veya erişim kısıtlı olduğunda yerleşik katalog + bildirim/AI verisi kullanılır
                }
            }
            discovered
        }

    companion object {
        private val tlAmountRegex = Regex("""(\d[\d.]*)\s*TL""", RegexOption.IGNORE_CASE)
        private val percentRegex = Regex("""%\s*(\d+)""")

        fun extractRewardAmount(text: String): Double {
            val matches = tlAmountRegex.findAll(text).mapNotNull {
                it.groupValues[1].replace(".", "").toDoubleOrNull()
            }.toList()

            return when {
                matches.size >= 2 -> matches.minOrNull() ?: 100.0
                matches.size == 1 -> matches.first().coerceAtMost(500.0)
                else -> 100.0
            }
        }

        fun extractMinSpendAmount(text: String, reward: Double): Double {
            val matches = tlAmountRegex.findAll(text).mapNotNull {
                it.groupValues[1].replace(".", "").toDoubleOrNull()
            }.toList()

            return when {
                matches.size >= 2 -> matches.maxOrNull() ?: (reward * 5)
                else -> (reward * 5).coerceAtLeast(250.0)
            }
        }

        fun extractPercentage(text: String): Double {
            return percentRegex.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        }

        fun guessCategory(text: String): SpendCategory {
            val lower = text.lowercase()
            return when {
                lower.contains("fatura") || lower.contains("turkcell") || lower.contains("telekom") -> SpendCategory.FATURA_OPERATOR
                lower.contains("market") || lower.contains("gıda") || lower.contains("süpermarket") -> SpendCategory.MARKET_GIDA
                lower.contains("akaryakıt") || lower.contains("benzin") || lower.contains("otogaz") -> SpendCategory.AKARYAKIT
                lower.contains("e-ticaret") || lower.contains("internet") || lower.contains("online") || lower.contains("pasaj") -> SpendCategory.ETICARET
                lower.contains("restoran") || lower.contains("kafe") || lower.contains("yemek") -> SpendCategory.RESTORAN_KAFE
                lower.contains("oyun") || lower.contains("netflix") || lower.contains("youtube") || lower.contains("spotify") -> SpendCategory.DIJITAL_OYUN
                lower.contains("eczane") || lower.contains("sağlık") -> SpendCategory.ECZANE_SAGLIK
                lower.contains("ulaşım") || lower.contains("istanbulkart") -> SpendCategory.ULASIM
                else -> SpendCategory.GENEL
            }
        }
    }
}
