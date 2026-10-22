package com.nakitradar.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nakitradar.app.data.AppRegistry
import com.nakitradar.app.data.WebCampaignScraper
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.CampaignSource
import com.nakitradar.app.model.CashbackType
import com.nakitradar.app.model.SpendCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedSpendNotification(
    val id: String,
    val appId: String,
    val appName: String,
    val merchantTitle: String,
    val spendAmount: Double,
    val category: SpendCategory
)

/**
 * Çift Modlu Bildirim Motoru:
 * 1) HARCAMA YAKALAYICI: Kasada veya internette ödeme yapıldığında banka/Paycell'den gelen
 *    "1.050 TL harcama yapıldı / ödendi" bildirimini yakalar, kampanya adımını ve kalan kotayı otomatik günceller.
 * 2) KAMPANYA YAKALAYICI: Uygulamalardan gelen "Sana Özel Nakit İade" bildirimlerini yakalar.
 */
class CampaignNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return

        val matchedApp = AppRegistry.findByPackageName(packageName) ?: return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        val fullBody = if (bigText.isNotEmpty()) bigText else text
        val combined = "$title $fullBody"

        // 1. Önce bu bildirimin bir HARCAMA / ÖDEME dekont bildirimi olup olmadığını kontrol et
        if (looksLikeSpendTransactionNotification(combined)) {
            val spendAmount = extractTransactionAmount(combined)
            if (spendAmount > 0.0) {
                val category = guessMerchantCategory(combined)
                val merchant = extractMerchantLabel(title, fullBody, category)
                emitSpendEvent(
                    CapturedSpendNotification(
                        id = "spend_${matchedApp.id}_${sbn.postTime}",
                        appId = matchedApp.id,
                        appName = matchedApp.name,
                        merchantTitle = merchant,
                        spendAmount = spendAmount,
                        category = category
                    )
                )
                return
            }
        }

        // 2. Harcama değilse YENİ KAMPANYA bildirimi olup olmadığını kontrol et
        if (!looksLikeCampaignNotification(combined)) return

        val reward = WebCampaignScraper.extractRewardAmount(combined)
        val minSpend = WebCampaignScraper.extractMinSpendAmount(combined, reward)
        val rate = WebCampaignScraper.extractPercentage(combined)

        val newCampaign = Campaign(
            id = "notif_${matchedApp.id}_${sbn.postTime}",
            appId = matchedApp.id,
            appName = matchedApp.name,
            title = title.ifEmpty { "${matchedApp.name} Kişiye Özel Bildirim Kampanyası" },
            description = fullBody,
            whereToFindInApp = "${matchedApp.name} > Bildirimler / Bana Özel (${matchedApp.hiddenCampaignPathHint})",
            category = WebCampaignScraper.guessCategory(combined),
            cashbackType = CashbackType.DIRECT_CASHBACK,
            minSpendAmount = minSpend,
            requiredTransactionCount = 1,
            cashbackRatePercent = rate,
            flatCashbackAmount = reward,
            maxCashbackCap = reward,
            requiresPreJoin = combined.contains("katıl", ignoreCase = true),
            validUntil = "Bildirim Özel Fırsatı",
            source = CampaignSource.NOTIFICATION_CAPTURED
        )

        addCapturedCampaign(newCampaign)
    }

    private fun looksLikeSpendTransactionNotification(text: String): Boolean {
        val lower = text.lowercase()
        val spendKeywords = listOf(
            "harcama yapıldı", "harcama yapılmıştır", "işlem gerçekleştirilmiştir",
            "işyeri", "işyerinden", "kartınızla", "kartınız ile",
            "tutarında harcama", "ödeme yapıldı", "faturanız ödendi",
            "ödeme işleminiz", "satış işlemi"
        )
        return spendKeywords.any { lower.contains(it) }
    }

    private fun looksLikeCampaignNotification(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf(
            "nakit iade", "cashback", "iade kazan", "tl puan", "bonus",
            "chip-para", "worldpuan", "maxipuan", "bankkart lira",
            "paycell", "hediye nakit", "harcamanıza", "faturanıza"
        )
        return keywords.any { lower.contains(it) }
    }

    companion object {
        private val amountRegex = Regex("""(\d[\d.]*(?:,\d{1,2})?)\s*TL""", RegexOption.IGNORE_CASE)

        private val _capturedNotifications = MutableStateFlow<List<Campaign>>(emptyList())
        val capturedNotifications: StateFlow<List<Campaign>> = _capturedNotifications.asStateFlow()

        private val _capturedSpendEvents = MutableStateFlow<List<CapturedSpendNotification>>(emptyList())
        val capturedSpendEvents: StateFlow<List<CapturedSpendNotification>> = _capturedSpendEvents.asStateFlow()

        fun addCapturedCampaign(campaign: Campaign) {
            val current = _capturedNotifications.value
            if (current.none { it.title == campaign.title && it.appId == campaign.appId }) {
                _capturedNotifications.value = listOf(campaign) + current
            }
        }

        fun emitSpendEvent(event: CapturedSpendNotification) {
            val current = _capturedSpendEvents.value
            if (current.none { it.id == event.id }) {
                _capturedSpendEvents.value = listOf(event) + current
            }
        }

        fun extractTransactionAmount(text: String): Double {
            val match = amountRegex.find(text)?.groupValues?.getOrNull(1) ?: return 0.0
            val normalized = match.replace(".", "").replace(",", ".")
            return normalized.toDoubleOrNull() ?: 0.0
        }

        fun guessMerchantCategory(text: String): SpendCategory {
            val lower = text.lowercase()
            return when {
                listOf("migros", "şok", "sok", "a101", "bim", "carrefour", "file", "macrocenter", "market", "gıda").any { lower.contains(it) } ->
                    SpendCategory.MARKET_GIDA
                listOf("opet", "shell", "petrol ofisi", "bp", "aytemiz", "total", "akaryakıt", "benzin").any { lower.contains(it) } ->
                    SpendCategory.AKARYAKIT
                listOf("trendyol", "hepsiburada", "amazon", "n11", "pazarama", "pasaj", "e-ticaret").any { lower.contains(it) } ->
                    SpendCategory.ETICARET
                listOf("turkcell", "superonline", "fatura", "telekom", "vodafone", "iski", "igdaş", "enerjisa").any { lower.contains(it) } ->
                    SpendCategory.FATURA_OPERATOR
                listOf("netflix", "spotify", "youtube", "steam", "playstation", "apple", "google play", "oyun").any { lower.contains(it) } ->
                    SpendCategory.DIJITAL_OYUN
                listOf("eczane", "hastane", "sağlık", "optik").any { lower.contains(it) } ->
                    SpendCategory.ECZANE_SAGLIK
                listOf("istanbulkart", "ulaşım", "bilet", "taksi", "martı").any { lower.contains(it) } ->
                    SpendCategory.ULASIM
                else -> WebCampaignScraper.guessCategory(text)
            }
        }

        fun extractMerchantLabel(title: String, body: String, category: SpendCategory): String {
            val combined = "$title $body"
            val knownMerchants = listOf(
                "MİGROS", "A101", "ŞOK MARKET", "BİM", "CARREFOURSA",
                "OPET", "SHELL", "PETROL OFİSİ", "TRENDYOL", "HEPSİBURADA",
                "AMAZON", "TURKCELL PASAJ", "TURKCELL FATURA", "NETFLIX", "YOUTUBE", "ECZANE"
            )
            val found = knownMerchants.firstOrNull { combined.contains(it, ignoreCase = true) }
            return found ?: "${category.displayName} Harcaması"
        }
    }
}
