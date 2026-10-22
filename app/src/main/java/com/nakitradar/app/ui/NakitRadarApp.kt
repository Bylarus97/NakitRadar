package com.nakitradar.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nakitradar.app.data.InstalledAppScanner
import com.nakitradar.app.model.*
import kotlin.math.roundToInt

@Composable
fun NakitRadarApp(
    viewModel: NakitRadarViewModel,
    scanner: InstalledAppScanner
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Strateji Motoru", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ViewList, contentDescription = null) },
                    label = { Text("Kampanyalar", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("Turkcell+Paycell", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("AI & Cihaz", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            TopCashbackDashboard(
                state = state,
                onScanApps = { viewModel.scanDeviceApps(scanner) },
                onRefreshWeb = { viewModel.refreshWebCampaigns() }
            )

            state.statusBannerMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            when (selectedTab) {
                0 -> StrategySimulatorScreen(
                    state = state,
                    onUpdateSimulation = viewModel::updateSimulationParameters,
                    onOpenApp = { appId ->
                        state.apps.find { it.id == appId }?.let { scanner.openAppOrCampaignPage(it) }
                    }
                )
                1 -> AllCampaignsDiscoveryScreen(
                    state = state,
                    onSelectFilterApp = viewModel::selectFilterApp,
                    onToggleJoin = viewModel::toggleJoinCampaign,
                    onOpenApp = { appId ->
                        state.apps.find { it.id == appId }?.let { scanner.openAppOrCampaignPage(it) }
                    }
                )
                2 -> TurkcellPaycellSynergyScreen(
                    state = state,
                    onToggleJoin = viewModel::toggleJoinCampaign,
                    onOpenApp = { appId ->
                        state.apps.find { it.id == appId }?.let { scanner.openAppOrCampaignPage(it) }
                    }
                )
                3 -> AiAndDeviceControlScreen(
                    state = state,
                    context = context,
                    onToggleApp = viewModel::toggleAppSelection,
                    onImportCampaign = viewModel::importCampaignFromText,
                    onUpdateApiKey = viewModel::updateGeminiApiKey,
                    onAskAi = viewModel::askAiAdvisor
                )
            }
        }
    }
}

@Composable
private fun TopCashbackDashboard(
    state: NakitRadarUiState,
    onScanApps: () -> Unit,
    onRefreshWeb: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NakitRadar • Nakit İade & Kombo Üssü",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Aktif ${state.activeAppIds.size} Uygulama • ${state.visibleCampaigns.size} Nakit İade Fırsatı",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onScanApps,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cihazı Tara", fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = onRefreshWeb,
                        enabled = !state.isScanningWeb,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.isScanningWeb) "..." else "Güncelle", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatHighlightBox(
                    modifier = Modifier.weight(1f),
                    title = "Aylık Maks. İade Potansiyeli",
                    value = "${state.totalMonthlyCashbackPotential.roundToInt()} TL",
                    accentColor = MaterialTheme.colorScheme.primary
                )
                StatHighlightBox(
                    modifier = Modifier.weight(1f),
                    title = "Katıldığım Kampanya İadesi",
                    value = "${state.joinedCampaignsCashbackTotal.roundToInt()} TL",
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StatHighlightBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, color = accentColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

@Composable
private fun StrategySimulatorScreen(
    state: NakitRadarUiState,
    onUpdateSimulation: (Double, SpendCategory) -> Unit,
    onOpenApp: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🎯 Harcama & Kombo Strateji Simülatörü",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Yapacağınız harcamanın sektörünü ve tutarını seçin; hangi uygulamaları nasıl birleştirerek (Stacking) veya bölerek en yüksek nakit iade alacağınızı görün.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    // Sektör Seçimi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpendCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick = { onUpdateSimulation(state.simulatedSpendAmount, cat) },
                                label = { Text("${cat.iconEmoji} ${cat.displayName}", fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Harcama Tutarı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Planlanan Harcama Tutarı:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.simulatedSpendAmount.roundToInt()} TL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = state.simulatedSpendAmount.toFloat(),
                        onValueChange = { onUpdateSimulation((it / 50f).roundToInt() * 50.0, state.selectedCategory) },
                        valueRange = 200f..5000f,
                        steps = 95
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(500.0, 1000.0, 1500.0, 2250.0, 3500.0).forEach { preset ->
                            OutlinedButton(
                                onClick = { onUpdateSimulation(preset, state.selectedCategory) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text("${preset.toInt()}₺", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        if (state.generatedStrategies.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Bu tutar ve kategori için alt limiti karşılayan aktif kampanya bulunamadı. Tutarı artırmayı veya 'AI & Cihaz' sekmesinden yeni uygulama aktifleştirmeyi deneyin.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(state.generatedStrategies, key = { it.id }) { plan ->
                StrategyPlanCard(plan = plan, onOpenApp = onOpenApp)
            }
        }
    }
}

@Composable
private fun StrategyPlanCard(
    plan: StrategyPlan,
    onOpenApp: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = plan.strategyType.badge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+${plan.totalCashbackEarned.roundToInt()} TL İADE (%${plan.effectiveReturnPercent.roundToInt()})",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = plan.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = plan.subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            plan.steps.forEach { step ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${step.stepNumber}. ${step.actionTitle}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+${step.earnedCashback.roundToInt()} TL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = step.actionDetail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "📍 Uygulama İçi Konum: ${step.inAppNavigationPath}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onOpenApp(step.appId) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${step.appName} Uygulamasını Aç & Katıl", fontSize = 11.sp)
                    }
                }
            }

            if (plan.warningNotes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                plan.warningNotes.forEach { note ->
                    Text(
                        text = "💡 $note",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AllCampaignsDiscoveryScreen(
    state: NakitRadarUiState,
    onSelectFilterApp: (String?) -> Unit,
    onToggleJoin: (String) -> Unit,
    onOpenApp: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Uygulama Filtre Çubuğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedFilterAppId == null,
                onClick = { onSelectFilterApp(null) },
                label = { Text("Tümü (${state.campaigns.count { it.appId in state.activeAppIds }})") }
            )
            state.apps.filter { it.isSelectedByUser }.forEach { app ->
                FilterChip(
                    selected = state.selectedFilterAppId == app.id,
                    onClick = { onSelectFilterApp(app.id) },
                    label = { Text(app.name) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.visibleCampaigns, key = { it.id }) { cam ->
                CampaignDetailCard(
                    campaign = cam,
                    onToggleJoin = { onToggleJoin(cam.id) },
                    onOpenApp = { onOpenApp(cam.appId) }
                )
            }
        }
    }
}

@Composable
private fun CampaignDetailCard(
    campaign: Campaign,
    onToggleJoin: () -> Unit,
    onOpenApp: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = campaign.appName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = campaign.source.badge,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Maks. ${campaign.maxCashbackCap.roundToInt()} TL İade",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${campaign.category.iconEmoji} ${campaign.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = campaign.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Uygulama İçi Gizli Konum Kutusu
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🧭 Uygulamada Nerede Bulunur? (Gizli Sekme Yolu):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = campaign.whereToFindInApp,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (campaign.synergyExplanation != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ Kombo Fırsatı: ${campaign.synergyExplanation}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alt Limit: ${campaign.minSpendAmount.roundToInt()} TL • ${if (campaign.requiresPreJoin) "Önce Katılım Şart!" else "Otomatik Katılım"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenApp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Uygulamayı Aç", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onToggleJoin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (campaign.isJoined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (campaign.isJoined) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (campaign.isJoined) "✓ Katıldım" else "Katıldım İşaretle", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TurkcellPaycellSynergyScreen(
    state: NakitRadarUiState,
    onToggleJoin: (String) -> Unit,
    onOpenApp: (String) -> Unit
) {
    val tcellPaycellCampaigns = state.campaigns.filter {
        it.appId == "turkcell" || it.appId == "paycell"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ Turkcell + Paycell + Banka 3'lü Sinerji Rehberi",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Turkcell ve Paycell uygulamalarında kampanyalar 4 farklı menüye dağılmış durumdadır. Maksimum nakit iade için aşağıdaki 3 adımlı zinciri uygulayın:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1️⃣ Banka Uygulaması (BonusFlaş / Juzdan / World): E-Ticaret veya Fatura kampanyasına 'Hemen Katıl' yapın.\n" +
                            "2️⃣ Paycell Uygulaması: 'Fırsatlar' sekmesinden Fatura (75 TL), Market (150 TL), Akaryakıt (175 TL) ve Dijital (%50) kampanyalarının tamamında 'Hemen Katıl' butonuna basın.\n" +
                            "3️⃣ Turkcell Uygulaması: 'Fatura' ve 'Pasaj' sekmelerindeki ödemelerde ödeme aracını 'Paycell Kart / Paycell Cüzdan' seçerek her iki uygulamanın nakit iadesini aynı anda tetikleyin!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(tcellPaycellCampaigns, key = { it.id }) { cam ->
            CampaignDetailCard(
                campaign = cam,
                onToggleJoin = { onToggleJoin(cam.id) },
                onOpenApp = { onOpenApp(cam.appId) }
            )
        }
    }
}

@Composable
private fun AiAndDeviceControlScreen(
    state: NakitRadarUiState,
    context: Context,
    onToggleApp: (String) -> Unit,
    onImportCampaign: (String, String) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onAskAi: (String) -> Unit
) {
    var rawCampaignText by remember { mutableStateOf("") }
    var selectedImportAppId by remember { mutableStateOf("paycell") }
    var aiQuestion by remember {
        mutableStateOf("Turkcell faturam 600 TL, bu ay 2.500 TL market ve 1.500 TL e-ticaret harcamam var. Paycell ve bankalarla nasıl maksimum nakit iade alırım?")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Bildirim Dinleyici & Otomatik Kampanya Yakalama
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔔 Otomatik Bildirimden Kampanya Yakalayıcı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Banka, Turkcell ve Paycell uygulamalarından gelen 'Sana Özel Nakit İade' bildirimlerini otomatik okuyup strateji motoruna eklemek için Bildirim Erişimi izni verebilirsiniz.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Android Bildirim Yakalayıcı İznini Aç", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Uygulama İçi / SMS / Ekran Görüntüsü Kampanya Metni Ekleme
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📥 Gizli / Kişiye Özel Kampanya Ekle (Metin / OCR)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Uygulama içinde 'Bana Özel' sekmesinde gördüğünüz veya SMS gelen bir kampanyayı aşağıya yapıştırın; tutar, alt limit ve iade oranını otomatik çözüp stratejiye dahil edelim.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.apps.forEach { app ->
                            FilterChip(
                                selected = selectedImportAppId == app.id,
                                onClick = { selectedImportAppId = app.id },
                                label = { Text(app.shortBadge, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = rawCampaignText,
                        onValueChange = { rawCampaignText = it },
                        placeholder = {
                            Text(
                                "Örn: Paycell Kart ile marketlerde yapacağınız 750 TL ve üzeri harcamaya 150 TL nakit iade! Hemen katılın.",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onImportCampaign(rawCampaignText, selectedImportAppId)
                            rawCampaignText = ""
                        },
                        enabled = rawCampaignText.isNotBlank()
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kampanyayı Çözümle ve Stratejiye Ekle")
                    }
                }
            }
        }

        // 3. AI Strateji Danışmanı (Gemini API + Yerleşik Uzman)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🤖 AI Nakit İade Strateji Danışmanı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.geminiApiKey,
                        onValueChange = onUpdateApiKey,
                        label = { Text("İsteğe Bağlı: Gemini API Anahtarı (Boş bırakılırsa Yerleşik Uzman çalışır)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiQuestion,
                        onValueChange = { aiQuestion = it },
                        label = { Text("Harcama Planınızı Yazın") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onAskAi(aiQuestion) },
                        enabled = !state.isAiLoading
                    ) {
                        Text(if (state.isAiLoading) "Strateji Hesaplanıyor..." else "Bana Özel Nakit İade Stratejisi Oluştur")
                    }

                    if (state.aiAdvisorResponse.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.aiAdvisorResponse,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Telefonumda Yüklü / Aktif Uygulamalar Listesi
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📱 Telefonumdaki Banka, Operatör ve Cüzdan Uygulamaları",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Kullandığınız uygulamaları işaretleyin; strateji motoru sadece seçtiğiniz uygulamalarla kombo oluşturur.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    state.apps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleApp(app.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(app.brandColorHex))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = app.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    if (app.isInstalledOnDevice) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "(Cihazda Yüklü)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = app.hiddenCampaignPathHint,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = app.isSelectedByUser,
                                onCheckedChange = { onToggleApp(app.id) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
