package com.nakitradar.app.domain

import com.nakitradar.app.model.AppEcosystemType
import com.nakitradar.app.model.Campaign
import com.nakitradar.app.model.InstalledAppInfo
import com.nakitradar.app.model.SpendCategory
import com.nakitradar.app.model.StrategyPlan
import com.nakitradar.app.model.StrategyStep
import com.nakitradar.app.model.StrategyType
import kotlin.math.roundToInt

/**
 * NakitRadar Strateji Motoru:
 * Telefonda yüklü olan uygulamalar arasındaki görünmeyen sinerjileri (Turkcell + Paycell + Banka),
 * Cüzdan Yükleme / Kart Kombosu (Stacking) fırsatlarını ve Harcama Bölme (Split Payment) taktiklerini
 * hesaplayarak maksimum nakit iade (cashback) planı üretir.
 */
class StrategyEngine {

    fun generateStrategies(
        spendAmount: Double,
        category: SpendCategory,
        enabledApps: List<InstalledAppInfo>,
        allCampaigns: List<Campaign>
    ): List<StrategyPlan> {
        if (spendAmount <= 0.0) return emptyList()

        val enabledAppIds = enabledApps.filter { it.isSelectedByUser }.map { it.id }.toSet()
        val appMap = enabledApps.associateBy { it.id }

        val relevantCampaigns = allCampaigns.filter {
            it.appId in enabledAppIds && (it.category == category || it.category == SpendCategory.GENEL)
        }

        val plans = mutableListOf<StrategyPlan>()

        // 1. EKOSİSTEM SİNERJİ STRATEJİSİ (Örn: Turkcell + Paycell Çifte İade Zinciri)
        buildSynergyStrategy(spendAmount, relevantCampaigns, allCampaigns, enabledAppIds)?.let {
            plans.add(it)
        }

        // 2. ÇİFTE KAZANÇ KOMBOSU (STACKING: Banka Kartı Kampanyası + Paycell/Nays Cüzdan İadesi)
        buildComboStackingStrategy(spendAmount, category, relevantCampaigns, appMap)?.let {
            plans.add(it)
        }

        // 3. HARCAMA BÖLME (SPLIT PAYMENT: Yüksek tutarlı harcamayı 2-3 uygulamaya bölerek tavan sınırını aşma)
        buildSplitSpendStrategy(spendAmount, relevantCampaigns)?.let {
            plans.add(it)
        }

        // 4. TEK İŞLEMDE EN İYİ KAMPANYA (Harcamayı bölmek istemeyenler için en karlı tek uygulama)
        buildSingleBestStrategy(spendAmount, relevantCampaigns)?.let {
            plans.add(it)
        }

        return plans.sortedByDescending { it.totalCashbackEarned }
    }

    private fun buildSynergyStrategy(
        spendAmount: Double,
        relevantCampaigns: List<Campaign>,
        allCampaigns: List<Campaign>,
        enabledAppIds: Set<String>
    ): StrategyPlan? {
        val synergyCam = relevantCampaigns.firstOrNull { cam ->
            cam.synergyPartnerAppId != null &&
                cam.synergyPartnerAppId in enabledAppIds &&
                spendAmount >= cam.minSpendAmount
        } ?: return null

        val partnerAppId = synergyCam.synergyPartnerAppId ?: return null
        val partnerCam = allCampaigns.firstOrNull {
            it.appId == partnerAppId && it.category == synergyCam.category
        }

        val mainCashback = synergyCam.calculateCashbackForSpend(spendAmount)
        val bonusCashback = synergyCam.synergyBonusAmount
        val totalCashback = mainCashback + bonusCashback
        val returnRate = ((totalCashback / spendAmount) * 100.0).coerceAtMost(100.0)

        val steps = mutableListOf<StrategyStep>()
        if (partnerCam != null) {
            steps.add(
                StrategyStep(
                    stepNumber = 1,
                    appId = partnerCam.appId,
                    appName = partnerCam.appName,
                    actionTitle = "${partnerCam.appName} Kampanyasına Katıl",
                    actionDetail = "Ödeme yapmadan önce ${partnerCam.appName} uygulamasını açıp '${partnerCam.title}' kampanyasında 'Hemen Katıl' butonuna basın.",
                    inAppNavigationPath = partnerCam.whereToFindInApp,
                    earnedCashback = bonusCashback
                )
            )
        }
        steps.add(
            StrategyStep(
                stepNumber = steps.size + 1,
                appId = synergyCam.appId,
                appName = synergyCam.appName,
                actionTitle = "${synergyCam.appName} İçinden Paycell ile İşlemi Tamamla",
                actionDetail = synergyCam.synergyExplanation ?: synergyCam.description,
                inAppNavigationPath = synergyCam.whereToFindInApp,
                earnedCashback = mainCashback
            )
        )

        return StrategyPlan(
            id = "plan_synergy_${synergyCam.id}",
            strategyType = StrategyType.ECOSYSTEM_SYNERGY,
            title = "${synergyCam.appName} + ${partnerCam?.appName ?: "Paycell"} Sinerji Zinciri",
            subtitle = "İki kardeş uygulamanın kampanyasını aynı harcamada tetikleyerek tek işlemde çifte nakit iade alın!",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = totalCashback,
            effectiveReturnPercent = returnRate,
            steps = steps,
            involvedCampaigns = listOfNotNull(synergyCam, partnerCam),
            warningNotes = listOf(
                "Ödemeyi yapmadan ÖNCE her iki uygulamada da 'Hemen Katıl' butonuna basılmış olmalıdır.",
                "Paycell Hediye Dünyası'ndaki puanlarınızı da nakit iade / fatura indirimine çevirmeyi unutmayın."
            )
        )
    }

    private fun buildComboStackingStrategy(
        spendAmount: Double,
        category: SpendCategory,
        relevantCampaigns: List<Campaign>,
        appMap: Map<String, InstalledAppInfo>
    ): StrategyPlan? {
        val walletCampaigns = relevantCampaigns.filter { cam ->
            appMap[cam.appId]?.ecosystemType == AppEcosystemType.PREPAID_WALLET &&
                spendAmount >= cam.minSpendAmount
        }
        val bankCampaigns = relevantCampaigns.filter { cam ->
            appMap[cam.appId]?.ecosystemType == AppEcosystemType.BANK_CAMPAIGN_APP &&
                cam.canStackAsWalletTopUp &&
                spendAmount >= cam.minSpendAmount
        }

        val bestWalletCam = walletCampaigns.maxByOrNull { it.calculateCashbackForSpend(spendAmount) } ?: return null
        val bestBankCam = bankCampaigns.maxByOrNull { it.calculateCashbackForSpend(spendAmount) } ?: return null

        val walletReward = bestWalletCam.calculateCashbackForSpend(spendAmount)
        val bankReward = bestBankCam.calculateCashbackForSpend(spendAmount)
        val totalReward = walletReward + bankReward
        val returnRate = ((totalReward / spendAmount) * 100.0).coerceAtMost(100.0)

        return StrategyPlan(
            id = "plan_combo_${bestBankCam.id}_${bestWalletCam.id}",
            strategyType = StrategyType.COMBO_STACKING,
            title = "${bestBankCam.appName} + ${bestWalletCam.appName} Çifte İade Kombosu",
            subtitle = "Banka kartı kampanyası ile ön ödemeli nakit iade cüzdanını birleştirerek 2 ayrı kurumdan iade kazanın.",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = totalReward,
            effectiveReturnPercent = returnRate,
            steps = listOf(
                StrategyStep(
                    stepNumber = 1,
                    appId = bestBankCam.appId,
                    appName = bestBankCam.appName,
                    actionTitle = "1. Adım: ${bestBankCam.appName} Kampanyasına Katıl",
                    actionDetail = "${bestBankCam.appName} uygulamasında '${bestBankCam.title}' kampanyasına katılın.",
                    inAppNavigationPath = bestBankCam.whereToFindInApp,
                    earnedCashback = bankReward
                ),
                StrategyStep(
                    stepNumber = 2,
                    appId = bestWalletCam.appId,
                    appName = bestWalletCam.appName,
                    actionTitle = "2. Adım: ${bestWalletCam.appName} Kampanyasına Katıl & Ödemeyi Yap",
                    actionDetail = "${bestWalletCam.appName} içinde kampanyaya katılın, banka kartınızla ${bestWalletCam.appName} üzerinden/bakiyesiyle ${spendAmount.roundToInt()} TL ${category.displayName} harcamasını gerçekleştirin.",
                    inAppNavigationPath = bestWalletCam.whereToFindInApp,
                    earnedCashback = walletReward
                )
            ),
            involvedCampaigns = listOf(bestBankCam, bestWalletCam),
            warningNotes = listOf(
                "Bazı bankalar doğrudan 'bakiye yükleme' işlemlerini kampanya dışı tutabilir; bu durumda Paycell/cüzdan içine tanımlı kredi kartıyla doğrudan QR/Online ödeme yaparak her iki kampanyayı tetikleyin."
            )
        )
    }

    private fun buildSplitSpendStrategy(
        spendAmount: Double,
        relevantCampaigns: List<Campaign>
    ): StrategyPlan? {
        // Harcama bölme stratejisi en az 2 farklı kampanya varsa ve toplam tutar her ikisinin alt limitine yetiyorsa devreye girer
        val eligible = relevantCampaigns
            .filter { it.minSpendAmount <= spendAmount }
            .sortedByDescending { it.calculateCashbackForSpend(it.minSpendAmount) / it.minSpendAmount.coerceAtLeast(1.0) }

        if (eligible.size < 2) return null

        var remaining = spendAmount
        val steps = mutableListOf<StrategyStep>()
        val usedCampaigns = mutableListOf<Campaign>()
        var totalSplitCashback = 0.0

        for (cam in eligible) {
            if (remaining >= cam.minSpendAmount && steps.size < 3) {
                val optimalSlice = if (cam.cashbackRatePercent > 0.0) {
                    val maxEffectiveSpend = (cam.maxCashbackCap / (cam.cashbackRatePercent / 100.0))
                    remaining.coerceAtMost(maxEffectiveSpend).coerceAtLeast(cam.minSpendAmount)
                } else {
                    cam.minSpendAmount
                }

                if (remaining >= optimalSlice) {
                    val reward = cam.calculateCashbackForSpend(optimalSlice)
                    if (reward > 0.0) {
                        remaining -= optimalSlice
                        totalSplitCashback += reward
                        usedCampaigns.add(cam)
                        steps.add(
                            StrategyStep(
                                stepNumber = steps.size + 1,
                                appId = cam.appId,
                                appName = cam.appName,
                                actionTitle = "${optimalSlice.roundToInt()} TL'lik Kısmı ${cam.appName} ile Öde",
                                actionDetail = "'${cam.title}' kampanyasının alt limiti olan ${optimalSlice.roundToInt()} TL'yi bu kart/cüzdan ile ödeyerek tavan iadeyi (${reward.roundToInt()} TL) cebe indirin.",
                                inAppNavigationPath = cam.whereToFindInApp,
                                earnedCashback = reward
                            )
                        )
                    }
                }
            }
        }

        if (steps.size < 2) return null

        val bestSingle = relevantCampaigns.maxOfOrNull { it.calculateCashbackForSpend(spendAmount) } ?: 0.0
        if (totalSplitCashback <= bestSingle) return null

        val extraGain = totalSplitCashback - bestSingle
        val returnRate = ((totalSplitCashback / spendAmount) * 100.0).coerceAtMost(100.0)

        return StrategyPlan(
            id = "plan_split_${usedCampaigns.joinToString("_") { it.id }}",
            strategyType = StrategyType.SPLIT_SPEND,
            title = "Harcamayı ${steps.size} Parçaya Böl: +${extraGain.roundToInt()} TL Fazladan Nakit İade!",
            subtitle = "${spendAmount.roundToInt()} TL'yi tek karttan çekerseniz kampanya üst limiti nedeniyle sadece ${bestSingle.roundToInt()} TL alırsınız. Ödemeyi bölerek ${totalSplitCashback.roundToInt()} TL alın!",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = totalSplitCashback,
            effectiveReturnPercent = returnRate,
            steps = steps,
            involvedCampaigns = usedCampaigns,
            warningNotes = listOf(
                "Market kasasında veya online alışverişte (sepeti ikiye bölerek / cüzdan bakiyesi + kart kullanarak) ödemeyi kolayca parçalayabilirsiniz.",
                "Kalan ${remaining.roundToInt()} TL tutar varsa puan oranı en yüksek kartınızla tamamlayabilirsiniz."
            )
        )
    }

    private fun buildSingleBestStrategy(
        spendAmount: Double,
        relevantCampaigns: List<Campaign>
    ): StrategyPlan? {
        val bestPair = relevantCampaigns
            .map { cam -> cam to cam.calculateCashbackForSpend(spendAmount) }
            .filter { it.second > 0.0 }
            .maxByOrNull { it.second } ?: return null

        val (cam, reward) = bestPair
        val returnRate = ((reward / spendAmount) * 100.0).coerceAtMost(100.0)

        return StrategyPlan(
            id = "plan_single_${cam.id}",
            strategyType = StrategyType.SINGLE_OPTIMAL,
            title = "Tek İşlemde En Karlı Seçim: ${cam.appName}",
            subtitle = "Harcamayı bölmeden tek seferde ödeyecekseniz en yüksek net iadeyi bu kampanya veriyor.",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = reward,
            effectiveReturnPercent = returnRate,
            steps = listOf(
                StrategyStep(
                    stepNumber = 1,
                    appId = cam.appId,
                    appName = cam.appName,
                    actionTitle = if (cam.requiresPreJoin) "Önce '${cam.appName}' Uygulamasından Katıl, Sonra Öde" else "${cam.appName} ile Doğrudan Öde (Otomatik Katılım)",
                    actionDetail = "${cam.description} (${spendAmount.roundToInt()} TL harcamaya net ${reward.roundToInt()} TL iade).",
                    inAppNavigationPath = cam.whereToFindInApp,
                    earnedCashback = reward
                )
            ),
            involvedCampaigns = listOf(cam),
            warningNotes = if (cam.requiresPreJoin) {
                listOf("DİKKAT: Bu kampanya ödeme öncesi '${cam.whereToFindInApp}' adımlarından 'Hemen Katıl' butonuna basılmasını şart koşuyor!")
            } else {
                listOf("Katılım şartı yoktur, doğrudan ödeme yaptığınızda nakit iade hesabınıza yansır.")
            }
        )
    }
}
