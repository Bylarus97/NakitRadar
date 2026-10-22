package com.nakitradar.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nakitradar.app.data.AppRegistry
import com.nakitradar.app.data.CampaignCatalog
import com.nakitradar.app.data.InstalledAppScanner
import com.nakitradar.app.data.WebCampaignScraper
import com.nakitradar.app.domain.GeminiStrategyAdvisor
import com.nakitradar.app.domain.StrategyEngine
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.InstalledAppInfo
import com.nakitradar.app.model.SpendCategory
import com.nakitradar.app.model.StrategyPlan
import com.nakitradar.app.service.CampaignNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NakitRadarUiState(
    val apps: List<InstalledAppInfo> = AppRegistry.supportedTurkishApps,
    val campaigns: List<Campaign> = CampaignCatalog.initialCampaigns,
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

    val totalMonthlyCashbackPotential: Double
        get() = campaigns.filter { it.appId in activeAppIds }.sumOf { it.maxCashbackCap }

    val joinedCampaignsCashbackTotal: Double
        get() = campaigns.filter { it.isJoined }.sumOf { it.maxCashbackCap }
}

class NakitRadarViewModel : ViewModel() {

    private val strategyEngine = StrategyEngine()
    private val webScraper = WebCampaignScraper()
    private val geminiAdvisor = GeminiStrategyAdvisor()

    private val _uiState = MutableStateFlow(NakitRadarUiState())
    val uiState: StateFlow<NakitRadarUiState> = _uiState.asStateFlow()

    init {
        recalculateStrategies()
        observeNotificationCampaigns()
    }

    private fun observeNotificationCampaigns() {
        viewModelScope.launch {
            CampaignNotificationListener.capturedNotifications.collect { notifCampaigns ->
                if (notifCampaigns.isNotEmpty()) {
                    _uiState.update { state ->
                        val existingIds = state.campaigns.map { it.id }.toSet()
                        val newOnes = notifCampaigns.filter { it.id !in existingIds }
                        state.copy(
                            campaigns = newOnes + state.campaigns,
                            statusBannerMessage = if (newOnes.isNotEmpty()) {
                                "Bildirimden ${newOnes.size} yeni nakit iade kampanyası yakalandı!"
                            } else state.statusBannerMessage
                        )
                    }
                    recalculateStrategies()
                }
            }
        }
    }

    fun scanDeviceApps(scanner: InstalledAppScanner) {
        val scanned = scanner.scanDeviceApps()
        val installedCount = scanned.count { it.isInstalledOnDevice }
        _uiState.update {
            it.copy(
                apps = scanned,
                statusBannerMessage = "Cihaz tarandı: $installedCount yüklü uygulama tespit edildi ve kampanyaları eşleştirildi."
            )
        }
        recalculateStrategies()
    }

    fun toggleAppSelection(appId: String) {
        _uiState.update { state ->
            val updatedApps = state.apps.map { app ->
                if (app.id == appId) app.copy(isSelectedByUser = !app.isSelectedByUser) else app
            }
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
                state.copy(
                    isScanningWeb = false,
                    campaigns = uniqueNew + state.campaigns,
                    statusBannerMessage = if (uniqueNew.isNotEmpty()) {
                        "${uniqueNew.size} yeni güncel web kampanyası eklendi!"
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
            state.copy(
                campaigns = listOf(parsed) + state.campaigns,
                statusBannerMessage = "'${parsed.title}' kampanyası analiz edildi ve Strateji Motoruna eklendi!"
            )
        }
        recalculateStrategies()
    }

    fun updateGeminiApiKey(key: String) {
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
