package com.nakitradar.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nakitradar.app.data.AppRegistry
import com.nakitradar.app.data.CampaignCatalog
import com.nakitradar.app.data.InstalledAppScanner
import com.nakitradar.app.data.LocalQuotaStorage
import com.nakitradar.app.data.WebCampaignScraper
import com.nakitradar.app.domain.GeminiStrategyAdvisor
import com.nakitradar.app.domain.StrategyEngine
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.InstalledAppInfo
import com.nakitradar.app.model.SpendCategory
import com.nakitradar.app.model.SpendTransactionLog
import com.nakitradar.app.model.StrategyPlan
import com.nakitradar.app.service.CampaignNotificationListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NakitRadarUiState(
    val apps: List<InstalledAppInfo> = AppRegistry.supportedTurkishApps,
    val campaigns: List<Campaign> = CampaignCatalog.initialCampaigns,
    val spendLogs: List<SpendTransactionLog> = emptyList(),
    val selectedFilterAppId: String? = null,
    val selectedCategory: SpendCategory = SpendCategory.MARKET_GIDA,
    val simulatedSpendAmount: Double = 2250.0,
    val generatedStrategies: List<StrategyPlan> = emptyList(),
    val isScanningWeb: Boolean = false,
    val statusBannerMessage: String? = null,
    val aiAdvisorResponse: String = "",
    val isAiLoading: Boolean = false,
    val geminiApiKey: String = ""
) {
    val activeAppIds: Set<String>
        get() = apps.filter { it.isSelectedByUser }.map { it.id }.toSet()

    val visibleCampaigns: List<Campaign>
        get() = campaigns.filter { cam ->
            cam.appId in activeAppIds &&
                (selectedFilterAppId == null || cam.appId == selectedFilterAppId)
        }

    /**
     * Aktif uygulamalarda henüz kotası dolmamış kalan toplam nakit iade potansiyeli (TL).
     */
    val totalRemainingCashbackPool: Double
        get() = campaigns.filter { it.appId in activeAppIds }.sumOf { it.remainingQuota }

    /**
     * Bu ay yapılan harcamalarla fiilen kazanılmış toplam nakit iade (TL).
     */
    val totalEarnedCashbackSoFar: Double
        get() = campaigns.sumOf { it.earnedSoFar }

    /**
     * Kotası tamamen dolup strateji motorundan elenen kampanya sayısı.
     */
    val fullQuotaCampaignCount: Int
        get() = campaigns.count { it.appId in activeAppIds && it.isQuotaFull }
}

class NakitRadarViewModel : ViewModel() {

    private val strategyEngine = StrategyEngine()
    private val webScraper = WebCampaignScraper()
    private val geminiAdvisor = GeminiStrategyAdvisor()
    private var quotaStorage: LocalQuotaStorage? = null
    private val processedSpendIds = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(NakitRadarUiState())
    val uiState: StateFlow<NakitRadarUiState> = _uiState.asStateFlow()

    init {
        recalculateStrategies()
        observeNotificationCampaigns()
        observeSpendNotificationEvents()
    }

    fun initializeStorage(storage: LocalQuotaStorage) {
        if (quotaStorage != null) return
        quotaStorage = storage

        val restoredCampaigns = storage.restoreCampaigns(CampaignCatalog.initialCampaigns)
        val restoredLogs = storage.restoreSpendLogs()
        val restoredEnabledIds = storage.restoreEnabledAppIds()
        val restoredApiKey = storage.restoreGeminiApiKey()

        _uiState.update { state ->
            val updatedApps = if (restoredEnabledIds != null) {
                state.apps.map { app -> app.copy(isSelectedByUser = app.id in restoredEnabledIds) }
            } else state.apps

            state.copy(
                apps = updatedApps,
                campaigns = restoredCampaigns,
                spendLogs = restoredLogs,
                geminiApiKey = restoredApiKey
            )
        }
        recalculateStrategies()
    }

    private fun observeNotificationCampaigns() {
        viewModelScope.launch {
            CampaignNotificationListener.capturedNotifications.collect { notifCampaigns ->
                if (notifCampaigns.isNotEmpty()) {
                    _uiState.update { state ->
                        val existingIds = state.campaigns.map { it.id }.toSet()
                        val newOnes = notifCampaigns.filter { it.id !in existingIds }
                        val merged = newOnes + state.campaigns
                        quotaStorage?.saveCampaignProgress(merged)
                        state.copy(
                            campaigns = merged,
                            statusBannerMessage = if (newOnes.isNotEmpty()) {
                                "🔔 Bildirimden ${newOnes.size} yeni nakit iade kampanyası yakalandı ve hafızaya kaydedildi!"
                            } else state.statusBannerMessage
                        )
                    }
                    recalculateStrategies()
                }
            }
        }
    }

    private fun observeSpendNotificationEvents() {
        viewModelScope.launch {
            CampaignNotificationListener.capturedSpendEvents.collect { spendEvents ->
                val latest = spendEvents.firstOrNull() ?: return@collect
                if (latest.id !in processedSpendIds) {
                    processedSpendIds.add(latest.id)
                    recordSpendAndDeductQuota(
                        appId = latest.appId,
                        spendAmount = latest.spendAmount,
                        category = latest.category,
                        merchantTitle = latest.merchantTitle,
                        isFromNotification = true
                    )
                }
            }
        }
    }

    /**
     * Kasada ödeme yapıldığında (bildirimden otomatik veya uygulama içinden manuel)
     * ilgili uygulamanın kampanyasını bulur, adım sayacını (+1) ve kazanılan iadeyi (earnedSoFar)
     * günceller; kota dolduysa strateji motorundan otomatik eler ve kalıcı hafızaya kaydeder.
     */
    fun recordSpendAndDeductQuota(
        appId: String,
        spendAmount: Double,
        category: SpendCategory,
        merchantTitle: String = "${category.displayName} Harcaması",
        targetCampaignId: String? = null,
        isFromNotification: Boolean = false
    ) {
        if (spendAmount <= 0.0) return

        val state = _uiState.value
        val appName = state.apps.find { it.id == appId }?.name ?: appId

        // Eşleşen kampanyayı bul (Önce doğrudan belirtilen ID, yoksa aynı uygulama + kategori ve kotası dolmamış olan)
        val matchedCampaign = if (targetCampaignId != null) {
            state.campaigns.find { it.id == targetCampaignId }
        } else {
            state.campaigns.filter {
                it.appId == appId &&
                    !it.isQuotaFull &&
                    (it.category == category || it.category == SpendCategory.GENEL) &&
                    spendAmount >= it.minSpendAmount
            }.maxByOrNull { it.calculateCashbackForSpend(spendAmount) }
        }

        val earnedCashback = matchedCampaign?.calculateCashbackForSpend(spendAmount) ?: 0.0
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val updatedCampaigns = state.campaigns.map { cam ->
            if (matchedCampaign != null && cam.id == matchedCampaign.id) {
                val newEarned = (cam.earnedSoFar + earnedCashback).coerceAtMost(cam.maxCashbackCap)
                val newSteps = cam.completedSteps + 1
                cam.copy(
                    isJoined = true,
                    completedSteps = newSteps,
                    earnedSoFar = newEarned
                )
            } else cam
        }

        val newLog = SpendTransactionLog(
            id = "log_${System.currentTimeMillis()}",
            appId = appId,
            appName = appName,
            merchantTitle = merchantTitle,
            spendAmount = spendAmount,
            cashbackEarned = earnedCashback,
            category = category,
            matchedCampaignId = matchedCampaign?.id,
            matchedCampaignTitle = matchedCampaign?.title,
            timestampLabel = timeStr,
            isAutoCapturedFromNotification = isFromNotification
        )

        val updatedLogs = listOf(newLog) + state.spendLogs
        quotaStorage?.saveCampaignProgress(updatedCampaigns)
        quotaStorage?.saveSpendLogs(updatedLogs)

        val updatedMatched = updatedCampaigns.find { it.id == matchedCampaign?.id }
        val bannerMsg = when {
            updatedMatched != null && updatedMatched.isQuotaFull ->
                "🎉 $appName (${spendAmount.roundToInt()} ₺) işlendi: +${earnedCashback.roundToInt()} ₺ iade! Bu kampanyanın kotası doldu ve strateji motorundan elendi."
            updatedMatched != null ->
                "✅ $appName (${spendAmount.roundToInt()} ₺) işlendi: +${earnedCashback.roundToInt()} ₺ iade! (Kalan Kota: ${updatedMatched.remainingQuota.roundToInt()} ₺)"
            else ->
                "ℹ️ $appName (${spendAmount.roundToInt()} ₺) harcaması kaydedildi (Alt limiti karşılayan veya kotası açık kampanya bulunamadı)."
        }

        _uiState.update {
            it.copy(
                campaigns = updatedCampaigns,
                spendLogs = updatedLogs,
                statusBannerMessage = bannerMsg
            )
        }
        recalculateStrategies()
    }

    fun resetCampaignQuota(campaignId: String) {
        _uiState.update { state ->
            val updated = state.campaigns.map { cam ->
                if (cam.id == campaignId) {
                    cam.copy(completedSteps = 0, earnedSoFar = 0.0)
                } else cam
            }
            quotaStorage?.saveCampaignProgress(updated)
            state.copy(
                campaigns = updated,
                statusBannerMessage = "Kampanya kotası ve adım sayacı sıfırlandı; strateji motoruna tekrar dahil edildi."
            )
        }
        recalculateStrategies()
    }

    fun scanDeviceApps(scanner: InstalledAppScanner) {
        val scanned = scanner.scanDeviceApps()
        val savedEnabledIds = quotaStorage?.restoreEnabledAppIds()
        val mergedApps = if (savedEnabledIds != null) {
            scanned.map { app -> app.copy(isSelectedByUser = app.id in savedEnabledIds || app.isInstalledOnDevice) }
        } else scanned

        val installedCount = mergedApps.count { it.isInstalledOnDevice }
        quotaStorage?.saveEnabledAppIds(mergedApps)
        _uiState.update {
            it.copy(
                apps = mergedApps,
                statusBannerMessage = "Cihaz tarandı: $installedCount yüklü uygulama ve kalan kotalar senkronize edildi."
            )
        }
        recalculateStrategies()
    }

    fun toggleAppSelection(appId: String) {
        _uiState.update { state ->
            val updatedApps = state.apps.map { app ->
                if (app.id == appId) app.copy(isSelectedByUser = !app.isSelectedByUser) else app
            }
            quotaStorage?.saveEnabledAppIds(updatedApps)
            state.copy(apps = updatedApps)
        }
        recalculateStrategies()
    }

    fun selectFilterApp(appId: String?) {
        _uiState.update { it.copy(selectedFilterAppId = appId) }
    }

    fun updateSimulationParameters(spendAmount: Double, category: SpendCategory) {
        _uiState.update {
            it.copy(
                simulatedSpendAmount = spendAmount,
                selectedCategory = category
            )
        }
        recalculateStrategies()
    }

    fun toggleJoinCampaign(campaignId: String) {
        _uiState.update { state ->
            val updated = state.campaigns.map { cam ->
                if (cam.id == campaignId) cam.copy(isJoined = !cam.isJoined) else cam
            }
            quotaStorage?.saveCampaignProgress(updated)
            state.copy(campaigns = updated)
        }
    }

    fun refreshWebCampaigns() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningWeb = true, statusBannerMessage = "Resmi kampanya sayfaları taranıyor...") }
            val liveCampaigns = webScraper.fetchLiveCampaignsForApps(_uiState.value.apps)
            _uiState.update { state ->
                val existingTitles = state.campaigns.map { it.title }.toSet()
                val uniqueNew = liveCampaigns.filter { it.title !in existingTitles }
                val merged = uniqueNew + state.campaigns
                quotaStorage?.saveCampaignProgress(merged)
                state.copy(
                    isScanningWeb = false,
                    campaigns = merged,
                    statusBannerMessage = if (uniqueNew.isNotEmpty()) {
                        "${uniqueNew.size} yeni güncel web kampanyası eklendi ve hafızaya kaydedildi!"
                    } else {
                        "Tüm yüklü uygulamaların güncel nakit iade kampanyaları senkronize edildi."
                    }
                )
            }
            recalculateStrategies()
        }
    }

    fun importCampaignFromText(rawText: String, appId: String) {
        if (rawText.isBlank()) return
        val app = _uiState.value.apps.find { it.id == appId } ?: _uiState.value.apps.first()
        val parsed = geminiAdvisor.parseCustomCampaignText(rawText, app)
        _uiState.update { state ->
            val merged = listOf(parsed) + state.campaigns
            quotaStorage?.saveCampaignProgress(merged)
            state.copy(
                campaigns = merged,
                statusBannerMessage = "'${parsed.title}' kampanyası kalıcı hafızaya ve Strateji Motoruna eklendi!"
            )
        }
        recalculateStrategies()
    }

    fun updateGeminiApiKey(key: String) {
        quotaStorage?.saveGeminiApiKey(key)
        _uiState.update { it.copy(geminiApiKey = key) }
    }

    fun askAiAdvisor(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val answer = geminiAdvisor.askAiStrategyAdvisor(
                userQuestion = question,
                installedApps = _uiState.value.apps,
                campaigns = _uiState.value.campaigns,
                geminiApiKey = _uiState.value.geminiApiKey
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiAdvisorResponse = answer
                )
            }
        }
    }

    private fun recalculateStrategies() {
        val state = _uiState.value
        val plans = strategyEngine.generateStrategies(
            spendAmount = state.simulatedSpendAmount,
            category = state.selectedCategory,
            enabledApps = state.apps,
            allCampaigns = state.campaigns
        )
        _uiState.update { it.copy(generatedStrategies = plans) }
    }
}
