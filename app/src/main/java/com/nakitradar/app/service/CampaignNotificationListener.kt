package com.nakitradar.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nakitradar.app.data.AppRegistry
import com.nakitradar.app.data.WebCampaignScraper
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.CampaignSource
import com.nakitradar.app.model.CashbackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Telefonda yüklü olan Turkcell, Paycell, BonusFlaş, Juzdan, World Mobil, Maximum, Nays, Papara vb.
 * uygulamalardan gelen anlık bildirimleri dinler; içinde "nakit iade", "TL", "puan", "hediye" geçen
 * kişiye özel kampanyaları otomatik yakalayıp strateji havuzuna ekler.
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
            whereToFindInApp = "${matchedApp.name} > Bildirimler / Bana Özel Sekmesi (${matchedApp.hiddenCampaignPathHint})",
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
        private val _capturedNotifications = MutableStateFlow<List<Campaign>>(emptyList())
        val capturedNotifications: StateFlow<List<Campaign>> = _capturedNotifications.asStateFlow()

        fun addCapturedCampaign(campaign: Campaign) {
            val current = _capturedNotifications.value
            if (current.none { it.title == campaign.title && it.appId == campaign.appId }) {
                _capturedNotifications.value = listOf(campaign) + current
            }
        }
    }
}
