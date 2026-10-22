package com.nakitradar.app.data

import android.content.Context
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.CampaignSource
import com.nakitradar.app.model.CashbackType
import com.nakitradar.app.model.InstalledAppInfo
import com.nakitradar.app.model.SpendCategory
import com.nakitradar.app.model.SpendTransactionLog
import org.json.JSONArray
import org.json.JSONObject

/**
 * Katılınan kampanyaları, kullanılan nakit iade kotalarını (earnedSoFar), tamamlanan adımları (completedSteps),
 * özel eklenen kampanyaları ve harcama geçmişini (SpendTransactionLog) cihaz hafızasında kalıcı olarak saklar.
 */
class LocalQuotaStorage(context: Context) {

    private val prefs = context.getSharedPreferences("nakit_radar_quota_prefs", Context.MODE_PRIVATE)

    fun saveCampaignProgress(campaigns: List<Campaign>) {
        val stateArray = JSONArray()
        val customArray = JSONArray()

        campaigns.forEach { cam ->
            val obj = JSONObject().apply {
                put("id", cam.id)
                put("isJoined", cam.isJoined)
                put("completedSteps", cam.completedSteps)
                put("earnedSoFar", cam.earnedSoFar)
            }
            stateArray.put(obj)

            // Bildirimden veya AI ile sonradan eklenen özel kampanyaları da tam olarak sakla
            if (cam.id.startsWith("notif_") || cam.id.startsWith("ai_") || cam.id.startsWith("live_")) {
                val customObj = JSONObject().apply {
                    put("id", cam.id)
                    put("appId", cam.appId)
                    put("appName", cam.appName)
                    put("title", cam.title)
                    put("description", cam.description)
                    put("whereToFindInApp", cam.whereToFindInApp)
                    put("category", cam.category.name)
                    put("minSpendAmount", cam.minSpendAmount)
                    put("requiredTransactionCount", cam.requiredTransactionCount)
                    put("cashbackRatePercent", cam.cashbackRatePercent)
                    put("flatCashbackAmount", cam.flatCashbackAmount)
                    put("maxCashbackCap", cam.maxCashbackCap)
                    put("requiresPreJoin", cam.requiresPreJoin)
                    put("source", cam.source.name)
                    put("isJoined", cam.isJoined)
                    put("completedSteps", cam.completedSteps)
                    put("earnedSoFar", cam.earnedSoFar)
                }
                customArray.put(customObj)
            }
        }

        prefs.edit()
            .putString(KEY_CAMPAIGN_STATES, stateArray.toString())
            .putString(KEY_CUSTOM_CAMPAIGNS, customArray.toString())
            .apply()
    }

    fun restoreCampaigns(defaultCatalog: List<Campaign>): List<Campaign> {
        val stateJson = prefs.getString(KEY_CAMPAIGN_STATES, null)
        val customJson = prefs.getString(KEY_CUSTOM_CAMPAIGNS, null)

        val progressMap = mutableMapOf<String, Triple<Boolean, Int, Double>>()
        if (!stateJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(stateJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    progressMap[o.getString("id")] = Triple(
                        o.optBoolean("isJoined", false),
                        o.optInt("completedSteps", 0),
                        o.optDouble("earnedSoFar", 0.0)
                    )
                }
            } catch (_: Exception) {
            }
        }

        val restoredCatalog = defaultCatalog.map { cam ->
            val saved = progressMap[cam.id]
            if (saved != null) {
                cam.copy(
                    isJoined = saved.first,
                    completedSteps = saved.second,
                    earnedSoFar = saved.third
                )
            } else cam
        }

        val customList = mutableListOf<Campaign>()
        if (!customJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(customJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val category = runCatching { SpendCategory.valueOf(o.getString("category")) }
                        .getOrDefault(SpendCategory.GENEL)
                    val source = runCatching { CampaignSource.valueOf(o.getString("source")) }
                        .getOrDefault(CampaignSource.AI_IMPORTED)

                    customList.add(
                        Campaign(
                            id = o.getString("id"),
                            appId = o.getString("appId"),
                            appName = o.getString("appName"),
                            title = o.getString("title"),
                            description = o.getString("description"),
                            whereToFindInApp = o.getString("whereToFindInApp"),
                            category = category,
                            cashbackType = CashbackType.DIRECT_CASHBACK,
                            minSpendAmount = o.optDouble("minSpendAmount", 500.0),
                            requiredTransactionCount = o.optInt("requiredTransactionCount", 1),
                            cashbackRatePercent = o.optDouble("cashbackRatePercent", 0.0),
                            flatCashbackAmount = o.optDouble("flatCashbackAmount", 100.0),
                            maxCashbackCap = o.optDouble("maxCashbackCap", 100.0),
                            requiresPreJoin = o.optBoolean("requiresPreJoin", true),
                            validUntil = "Kaydedilen Fırsat",
                            source = source,
                            isJoined = o.optBoolean("isJoined", false),
                            completedSteps = o.optInt("completedSteps", 0),
                            earnedSoFar = o.optDouble("earnedSoFar", 0.0)
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        val catalogIds = restoredCatalog.map { it.id }.toSet()
        return customList.filter { it.id !in catalogIds } + restoredCatalog
    }

    fun saveSpendLogs(logs: List<SpendTransactionLog>) {
        val arr = JSONArray()
        logs.take(50).forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("appId", log.appId)
                put("appName", log.appName)
                put("merchantTitle", log.merchantTitle)
                put("spendAmount", log.spendAmount)
                put("cashbackEarned", log.cashbackEarned)
                put("category", log.category.name)
                put("matchedCampaignId", log.matchedCampaignId ?: "")
                put("matchedCampaignTitle", log.matchedCampaignTitle ?: "")
                put("timestampLabel", log.timestampLabel)
                put("isAutoCapturedFromNotification", log.isAutoCapturedFromNotification)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SPEND_LOGS, arr.toString()).apply()
    }

    fun restoreSpendLogs(): List<SpendTransactionLog> {
        val raw = prefs.getString(KEY_SPEND_LOGS, null) ?: return emptyList()
        val list = mutableListOf<SpendTransactionLog>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cat = runCatching { SpendCategory.valueOf(o.getString("category")) }
                    .getOrDefault(SpendCategory.GENEL)
                list.add(
                    SpendTransactionLog(
                        id = o.getString("id"),
                        appId = o.getString("appId"),
                        appName = o.getString("appName"),
                        merchantTitle = o.getString("merchantTitle"),
                        spendAmount = o.optDouble("spendAmount", 0.0),
                        cashbackEarned = o.optDouble("cashbackEarned", 0.0),
                        category = cat,
                        matchedCampaignId = o.optString("matchedCampaignId").takeIf { it.isNotEmpty() },
                        matchedCampaignTitle = o.optString("matchedCampaignTitle").takeIf { it.isNotEmpty() },
                        timestampLabel = o.optString("timestampLabel", "Az önce"),
                        isAutoCapturedFromNotification = o.optBoolean("isAutoCapturedFromNotification", true)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun saveEnabledAppIds(apps: List<InstalledAppInfo>) {
        val enabledIds = apps.filter { it.isSelectedByUser }.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_ENABLED_APPS, enabledIds).apply()
    }

    fun restoreEnabledAppIds(): Set<String>? {
        return prefs.getStringSet(KEY_ENABLED_APPS, null)
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, key).apply()
    }

    fun restoreGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API, "") ?: ""
    }

    companion object {
        private const val KEY_CAMPAIGN_STATES = "campaign_states_v2"
        private const val KEY_CUSTOM_CAMPAIGNS = "custom_campaigns_v2"
        private const val KEY_SPEND_LOGS = "spend_logs_v2"
        private const val KEY_ENABLED_APPS = "enabled_apps_v2"
        private const val KEY_GEMINI_API = "gemini_api_key_v2"
    }
}
