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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nakitradar.app.data.InstalledAppScanner
import com.nakitradar.app.model.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NakitRadarApp(
    viewModel: NakitRadarViewModel,
    scanner: InstalledAppScanner
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeBottomSheetCampaign by remember { mutableStateOf<Campaign?>(null) }
    var activeBottomSheetPlan by remember { mutableStateOf<StrategyPlan?>(null) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        label = { Text("Cüzdan & Kombo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                        label = { Text("Kampanyalar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                        label = { Text("Turkcell+Paycell", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text("AI & Ayarlar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Ferah Fintech Üst Bar
            LightFintechHeader(
                state = state,
                onScanApps = { viewModel.scanDeviceApps(scanner) },
                onRefreshWeb = { viewModel.refreshWebCampaigns() }
            )

            // Yatay Kaydırılabilir Gerçek Cüzdan / Kart Karuseli (Apple Wallet / Juzdan Stili)
            WalletCardCarousel(
                state = state,
                onSelectCard = { appId ->
                    viewModel.selectFilterApp(appId)
                }
            )

            state.statusBannerMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> WalletComboHomeScreen(
                    state = state,
                    onUpdateSimulation = viewModel::updateSimulationParameters,
                    onInspectPlan = { activeBottomSheetPlan = it },
                    onOpenApp = { appId ->
                        state.apps.find { it.id == appId }?.let { scanner.openAppOrCampaignPage(it) }
                    }
                )
                1 -> CleanCampaignFeedScreen(
                    state = state,
                    onInspectCampaign = { activeBottomSheetCampaign = it },
                    onToggleJoin = viewModel::toggleJoinCampaign
                )
                2 -> CleanTurkcellPaycellScreen(
                    state = state,
                    onInspectCampaign = { activeBottomSheetCampaign = it },
                    onToggleJoin = viewModel::toggleJoinCampaign
                )
                3 -> CleanAiAndAppsScreen(
                    state = state,
                    context = context,
                    onToggleApp = viewModel::toggleAppSelection,
                    onImportCampaign = viewModel::importCampaignFromText,
                    onUpdateApiKey = viewModel::updateGeminiApiKey,
                    onAskAi = viewModel::askAiAdvisor
                )
            }
        }

        // Strateji Detay Bottom Sheet (Tıklayınca Açılan Temiz Pencere)
        activeBottomSheetPlan?.let { plan ->
            ModalBottomSheet(
                onDismissRequest = { activeBottomSheetPlan = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                StrategyDetailSheetContent(
                    plan = plan,
                    onOpenApp = { appId ->
                        state.apps.find { it.id == appId }?.let { scanner.openAppOrCampaignPage(it) }
                    },
                    onClose = { activeBottomSheetPlan = null }
                )
            }
        }

        // Kampanya Detay & Gizli Sekme Yolu Bottom Sheet
        activeBottomSheetCampaign?.let { cam ->
            ModalBottomSheet(
                onDismissRequest = { activeBottomSheetCampaign = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CampaignDetailSheetContent(
                    campaign = cam,
                    onToggleJoin = {
                        viewModel.toggleJoinCampaign(cam.id)
                        activeBottomSheetCampaign = cam.copy(isJoined = !cam.isJoined)
                    },
                    onOpenApp = {
                        state.apps.find { it.id == cam.appId }?.let { scanner.openAppOrCampaignPage(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun LightFintechHeader(
    state: NakitRadarUiState,
    onScanApps: () -> Unit,
    onRefreshWeb: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "NakitRadar",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Kazanılabilir Havuz: ${state.totalMonthlyCashbackPotential.roundToInt()} ₺ • Katıldığım: ${state.joinedCampaignsCashbackTotal.roundToInt()} ₺",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledIconButton(
                onClick = onScanApps,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.PhonelinkSetup, contentDescription = "Cihazı Tara", modifier = Modifier.size(18.dp))
            }
            FilledIconButton(
                onClick = onRefreshWeb,
                enabled = !state.isScanningWeb,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Güncelle", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WalletCardCarousel(
    state: NakitRadarUiState,
    onSelectCard: (String?) -> Unit
) {
    val activeApps = state.apps.filter { it.isSelectedByUser }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Tüm Cüzdanlar Özet Kartı
        item {
            val isAllSelected = state.selectedFilterAppId == null
            Box(
                modifier = Modifier
                    .width(175.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )
                    .border(
                        width = if (isAllSelected) 2.5.dp else 0.dp,
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectCard(null) }
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TÜM CÜZDANLAR",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${state.totalMonthlyCashbackPotential.roundToInt()} ₺",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${activeApps.size} Uygulama • ${state.campaigns.count { it.appId in state.activeAppIds }} Fırsat",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. Her Uygulama İçin Gerçekçi Banka/Cüzdan Kartı
        items(activeApps, key = { it.id }) { app ->
            val isSelected = state.selectedFilterAppId == app.id
            val appCampaigns = state.campaigns.filter { it.appId == app.id }
            val appTotalCashback = appCampaigns.sumOf { it.maxCashbackCap }.roundToInt()
            val gradientColors = getCardGradientForApp(app.id)

            Box(
                modifier = Modifier
                    .width(195.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(gradientColors))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        onSelectCard(if (isSelected) null else app.id)
                    }
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = app.shortBadge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Icon(
                            Icons.Default.Contactless,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = app.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${appCampaigns.size} Aktif Kampanya",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "+$appTotalCashback ₺",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCardGradientForApp(appId: String): List<Color> = when (appId) {
    "turkcell" -> listOf(Color(0xFFF59E0B), Color(0xFF1D4ED8))
    "paycell" -> listOf(Color(0xFF0284C7), Color(0xFF1E3A8A))
    "nays" -> listOf(Color(0xFF9333EA), Color(0xFF4C1D95))
    "papara" -> listOf(Color(0xFF18181B), Color(0xFF3F3F46))
    "bonusflas" -> listOf(Color(0xFF059669), Color(0xFF064E3B))
    "juzdan" -> listOf(Color(0xFFEF4444), Color(0xFF991B1B))
    "worldmobil" -> listOf(Color(0xFF7C3AED), Color(0xFF3B0764))
    "maximum" -> listOf(Color(0xFF2563EB), Color(0xFF1E40AF))
    "bankkart" -> listOf(Color(0xFFDC2626), Color(0xFF7F1D1D))
    else -> listOf(Color(0xFF334155), Color(0xFF0F172A))
}

@Composable
private fun WalletComboHomeScreen(
    state: NakitRadarUiState,
    onUpdateSimulation: (Double, SpendCategory) -> Unit,
    onInspectPlan: (StrategyPlan) -> Unit,
    onOpenApp: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kompakt & Ferah "Bugün Ne Harcayacaksın?" Hesaplayıcı Kartı
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ne Kadar Harcayacaksın?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "En yüksek nakit iade kombosunu anında bul",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${state.simulatedSpendAmount.roundToInt()} ₺",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Kategori Seçim Çipleri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpendCategory.entries.forEach { cat ->
                            val selected = state.selectedCategory == cat
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.clickable { onUpdateSimulation(state.simulatedSpendAmount, cat) }
                            ) {
                                Text(
                                    text = "${cat.iconEmoji} ${cat.displayName}",
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = state.simulatedSpendAmount.toFloat(),
                        onValueChange = { onUpdateSimulation((it / 50f).roundToInt() * 50.0, state.selectedCategory) },
                        valueRange = 200f..5000f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500.0, 1000.0, 1500.0, 2250.0, 3500.0).forEach { preset ->
                            val isPreset = state.simulatedSpendAmount.roundToInt() == preset.toInt()
                            Surface(
                                color = if (isPreset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateSimulation(preset, state.selectedCategory) }
                            ) {
                               Box(
                                   contentAlignment = Alignment.Center,
                                   modifier = Modifier.padding(vertical = 6.dp)
                               ) {
                                   Text(
                                       text = "${preset.toInt()}₺",
                                       fontSize = 12.sp,
                                       fontWeight = FontWeight.Bold,
                                       color = if (isPreset) Color.White else MaterialTheme.colorScheme.onSurface
                                   )
                               }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Önerilen Kombo & İade Stratejileri (${state.generatedStrategies.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(state.generatedStrategies, key = { it.id }) { plan ->
            VisualStrategyPipelineCard(
                plan = plan,
                onClick = { onInspectPlan(plan) },
                onQuickOpenFirstApp = {
                    plan.steps.firstOrNull()?.let { onOpenApp(it.appId) }
                }
            )
        }
    }
}

@Composable
private fun VisualStrategyPipelineCard(
    plan: StrategyPlan,
    onClick: () -> Unit,
    onQuickOpenFirstApp: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Üst Rozet Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = plan.strategyType.badge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "+${plan.totalCashbackEarned.roundToInt()} ₺ İADE",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = plan.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            // Uzun yazılar yerine Görsel Akış Şeması (Visual Flow Pipeline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plan.steps.forEachIndexed { index, step ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.stepNumber}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = step.appName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "+${step.earnedCashback.roundToInt()} ₺ kazan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (index < plan.steps.lastIndex) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net İade Oranı: %${plan.effectiveReturnPercent.roundToInt()} • Detaylar için dokun",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onClick) {
                    Text("Adımları Gör", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CleanCampaignFeedScreen(
    state: NakitRadarUiState,
    onInspectCampaign: (Campaign) -> Unit,
    onToggleJoin: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = if (state.selectedFilterAppId == null) {
                    "Tüm Aktif Kampanyalar (${state.visibleCampaigns.size})"
                } else {
                    "Seçili Cüzdan Kampanyaları (${state.visibleCampaigns.size})"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(state.visibleCampaigns, key = { it.id }) { cam ->
            MinimalCampaignCard(
                campaign = cam,
                onClick = { onInspectCampaign(cam) },
                onToggleJoin = { onToggleJoin(cam.id) }
            )
        }
    }
}

@Composable
private fun MinimalCampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    onToggleJoin: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = campaign.appName,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (campaign.synergyPartnerAppId != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚡ Kombo",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "+${campaign.maxCashbackCap.roundToInt()} ₺",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${campaign.category.iconEmoji} ${campaign.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // İlerleme / Alt Limit Çubuğu
            LinearProgressIndicator(
                progress = { if (campaign.isJoined) 1f else 0.25f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (campaign.isJoined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alt Limit: ${campaign.minSpendAmount.roundToInt()} ₺ • Gizli Sekmeyi Gör",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                FilterChip(
                    selected = campaign.isJoined,
                    onClick = onToggleJoin,
                    label = {
                        Text(
                            text = if (campaign.isJoined) "✓ Katıldım" else "Katıl İşaretle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CleanTurkcellPaycellScreen(
    state: NakitRadarUiState,
    onInspectCampaign: (Campaign) -> Unit,
    onToggleJoin: (String) -> Unit
) {
    val tcellPaycellCampaigns = state.campaigns.filter {
        it.appId == "turkcell" || it.appId == "paycell"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Turkcell + Paycell Çifte İade Akışı",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowBadgeStep("1. Banka", "Kampanyaya Katıl", Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        FlowBadgeStep("2. Paycell", "Fırsatlarda Katıl", Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        FlowBadgeStep("3. Turkcell", "Paycell ile Öde", Modifier.weight(1f))
                    }
                }
            }
        }

        items(tcellPaycellCampaigns, key = { it.id }) { cam ->
            MinimalCampaignCard(
                campaign = cam,
                onClick = { onInspectCampaign(cam) },
                onToggleJoin = { onToggleJoin(cam.id) }
            )
        }
    }
}

@Composable
private fun FlowBadgeStep(title: String, sub: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(horizontal = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = sub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CleanAiAndAppsScreen(
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
        mutableStateOf("Turkcell faturam 600 TL, bu ay 2.250 TL market harcamam var. Paycell ve bankalarla nasıl maksimum nakit iade alırım?")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📥 Yeni Kampanya Metni / SMS Ekle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawCampaignText,
                        onValueChange = { rawCampaignText = it },
                        placeholder = { Text("Örn: Paycell ile markette 750 TL harcamaya 150 TL nakit iade!", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onImportCampaign(rawCampaignText, selectedImportAppId)
                            rawCampaignText = ""
                        },
                        enabled = rawCampaignText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Strateji Motoruna Ekle")
                    }
                }
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🤖 AI Strateji Danışmanı", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiQuestion,
                        onValueChange = { aiQuestion = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onAskAi(aiQuestion) },
                        enabled = !state.isAiLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (state.isAiLoading) "Hesaplanıyor..." else "Bana Özel Kombo Planı Çıkar")
                    }
                    if (state.aiAdvisorResponse.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.aiAdvisorResponse,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📱 Cüzdanımdaki Uygulamalar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Bildirim İzni", fontSize = 12.sp)
                        }
                    }
                    state.apps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Switch(
                                checked = app.isSelectedByUser,
                                onCheckedChange = { onToggleApp(app.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyDetailSheetContent(
    plan: StrategyPlan,
    onOpenApp: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = plan.title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = plan.subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        plan.steps.forEach { step ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${step.stepNumber}. ${step.actionTitle}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+${step.earnedCashback.roundToInt()} ₺",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(text = step.actionDetail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "🧭 Gizli Menü Yolu: ${step.inAppNavigationPath}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onOpenApp(step.appId) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("${step.appName} Uygulamasını Aç")
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CampaignDetailSheetContent(
    campaign: Campaign,
    onToggleJoin: () -> Unit,
    onOpenApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "${campaign.appName} • Maks. ${campaign.maxCashbackCap.roundToInt()} ₺ Nakit İade",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = campaign.title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = campaign.description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🧭 Uygulamada Hangi Sekmede Bulunur?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = campaign.whereToFindInApp,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        campaign.synergyExplanation?.let { syn ->
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚡ Kombo İpucu: $syn",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onToggleJoin,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (campaign.isJoined) "✓ Katıldım" else "Katıldım İşaretle")
            }
            Button(
                onClick = onOpenApp,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Uygulamayı Aç")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
