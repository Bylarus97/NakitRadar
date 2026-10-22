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
 * NakitRadar Strateji Motoru (v2 - Kalan Kota & Adım Duyarlı):
 * - Kotası dolan (isQuotaFull == true) kampanyaları otomatik olarak eler.
 * - Kısmen kullanılmış kampanyalarda sadece kalan kotayı (remainingQuota) hesaba katar.
 * - Turkcell + Paycell + Banka Sinerjisi, Çifte Kazanç Kombosu ve Harcama Bölme stratejileri üretir.
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

        // Sadece aktif uygulamalara ait, ilgili kategorideki ve KOTASI DOLMAMIŞ kampanyaları al
        val relevantCampaigns = allCampaigns.filter {
            it.appId in enabledAppIds &&
                !it.isQuotaFull &&
                (it.category == category || it.category == SpendCategory.GENEL)
        }

        val fullQuotaExcludedCount = allCampaigns.count {
            it.appId in enabledAppIds &&
                it.isQuotaFull &&
                (it.category == category || it.category == SpendCategory.GENEL)
        }

        val plans = mutableListOf<StrategyPlan>()

        // 1. EKOSİSTEM SİNERJİ STRATEJİSİ (Örn: Turkcell + Paycell Çifte İade Zinciri)
        buildSynergyStrategy(spendAmount, relevantCampaigns, allCampaigns, enabledAppIds, fullQuotaExcludedCount)?.let {
            plans.add(it)
        }

        // 2. ÇİFTE KAZANÇ KOMBOSU (STACKING: Banka Kartı Kampanyası + Paycell/Nays Cüzdan İadesi)
        buildComboStackingStrategy(spendAmount, category, relevantCampaigns, appMap, fullQuotaExcludedCount)?.let {
            plans.add(it)
        }

        // 3. HARCAMA BÖLME (SPLIT PAYMENT: Yüksek tutarlı harcamayı 2-3 uygulamaya bölerek kalan kotaları toplama)
        buildSplitSpendStrategy(spendAmount, relevantCampaigns, fullQuotaExcludedCount)?.let {
            plans.add(it)
        }

        // 4. TEK İŞLEMDE EN İYİ KAMPANYA
        buildSingleBestStrategy(spendAmount, relevantCampaigns, fullQuotaExcludedCount)?.let {
            plans.add(it)
        }

        return plans.sortedByDescending { it.totalCashbackEarned }
    }

    private fun buildSynergyStrategy(
        spendAmount: Double,
        relevantCampaigns: List<Campaign>,
        allCampaigns: List<Campaign>,
        enabledAppIds: Set<String>,
        excludedCount: Int
    ): StrategyPlan? {
        val synergyCam = relevantCampaigns.firstOrNull { cam ->
            cam.synergyPartnerAppId != null &&
                cam.synergyPartnerAppId in enabledAppIds &&
                spendAmount >= cam.minSpendAmount &&
                cam.calculateCashbackForSpend(spendAmount) > 0.0
        } ?: return null

        val partnerAppId = synergyCam.synergyPartnerAppId ?: return null
        val partnerCam = allCampaigns.firstOrNull {
            it.appId == partnerAppId && it.category == synergyCam.category && !it.isQuotaFull
        }

        val mainCashback = synergyCam.calculateCashbackForSpend(spendAmount)
        val bonusCashback = if (partnerCam != null) {
            synergyCam.synergyBonusAmount.coerceAtMost(partnerCam.remainingQuota)
        } else {
            synergyCam.synergyBonusAmount
        }

        if (bonusCashback <= 0.0) return null

        val totalCashback = mainCashback + bonusCashback
        val returnRate = ((totalCashback / spendAmount) * 100.0).coerceAtMost(100.0)

        val steps = mutableListOf<StrategyStep>()
        if (partnerCam != null) {
            steps.add(
                StrategyStep(
                    stepNumber = 1,
                    appId = partnerCam.appId,
                    appName = partnerCam.appName,
                    actionTitle = "${partnerCam.appName} Kampanyasına Katıl (Kalan Kota: ${partnerCam.remainingQuota.roundToInt()} ₺)",
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

        val notes = mutableListOf(
            "Ödemeyi yapmadan ÖNCE her iki uygulamada da 'Hemen Katıl' butonuna basılmış olmalıdır."
        )
        if (excludedCount > 0) {
            notes.add("Bu kategoride kotası dolmuş $excludedCount kampanya otomatik olarak elendi.")
        }

        return StrategyPlan(
            id = "plan_synergy_${synergyCam.id}",
            strategyType = StrategyType.ECOSYSTEM_SYNERGY,
            title = "${synergyCam.appName} + ${partnerCam?.appName ?: "Paycell"} Sinerji Zinciri",
            subtitle = "İki kardeş uygulamanın kalan kotasını aynı harcamada birleştirerek çifte nakit iade alın!",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = totalCashback,
            effectiveReturnPercent = returnRate,
            steps = steps,
            involvedCampaigns = listOfNotNull(synergyCam, partnerCam),
            warningNotes = notes
        )
    }

    private fun buildComboStackingStrategy(
        spendAmount: Double,
        category: SpendCategory,
        relevantCampaigns: List<Campaign>,
        appMap: Map<String, InstalledAppInfo>,
        excludedCount: Int
    ): StrategyPlan? {
        val walletCampaigns = relevantCampaigns.filter { cam ->
            appMap[cam.appId]?.ecosystemType == AppEcosystemType.PREPAID_WALLET &&
                spendAmount >= cam.minSpendAmount &&
                cam.calculateCashbackForSpend(spendAmount) > 0.0
        }
        val bankCampaigns = relevantCampaigns.filter { cam ->
            appMap[cam.appId]?.ecosystemType == AppEcosystemType.BANK_CAMPAIGN_APP &&
                cam.canStackAsWalletTopUp &&
                spendAmount >= cam.minSpendAmount &&
                cam.calculateCashbackForSpend(spendAmount) > 0.0
        }

        val bestWalletCam = walletCampaigns.maxByOrNull { it.calculateCashbackForSpend(spendAmount) } ?: return null
        val bestBankCam = bankCampaigns.maxByOrNull { it.calculateCashbackForSpend(spendAmount) } ?: return null

        val walletReward = bestWalletCam.calculateCashbackForSpend(spendAmount)
        val bankReward = bestBankCam.calculateCashbackForSpend(spendAmount)
        val totalReward = walletReward + bankReward
        val returnRate = ((totalReward / spendAmount) * 100.0).coerceAtMost(100.0)

        val notes = mutableListOf(
            "Cüzdan içine tanımlı kredi kartıyla QR/Online ödeme yaparak her iki kampanyayı aynı anda tetikleyin."
        )
        if (excludedCount > 0) {
            notes.add("Kotası dolmuş $excludedCount kampanya bu kombodan otomatik elendi.")
        }

        return StrategyPlan(
            id = "plan_combo_${bestBankCam.id}_${bestWalletCam.id}",
            strategyType = StrategyType.COMBO_STACKING,
            title = "${bestBankCam.appName} + ${bestWalletCam.appName} Çifte İade Kombosu",
            subtitle = "Banka kartı kalan kotası (${bestBankCam.remainingQuota.roundToInt()} ₺) ile cüzdan kalan kotasını (${bestWalletCam.remainingQuota.roundToInt()} ₺) birleştirin.",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = totalReward,
            effectiveReturnPercent = returnRate,
            steps = listOf(
                StrategyStep(
                    stepNumber = 1,
                    appId = bestBankCam.appId,
                    appName = bestBankCam.appName,
                    actionTitle = "1. Adım: ${bestBankCam.appName} Kampanyasına Katıl",
                    actionDetail = "${bestBankCam.appName} uygulamasında '${bestBankCam.title}' kampanyasına katılın (Kalan Kota: ${bestBankCam.remainingQuota.roundToInt()} ₺).",
                    inAppNavigationPath = bestBankCam.whereToFindInApp,
                    earnedCashback = bankReward
                ),
                StrategyStep(
                    stepNumber = 2,
                    appId = bestWalletCam.appId,
                    appName = bestWalletCam.appName,
                    actionTitle = "2. Adım: ${bestWalletCam.appName} ile Ödemeyi Yap",
                    actionDetail = "${bestWalletCam.appName} üzerinden ${spendAmount.roundToInt()} ₺ ${category.displayName} harcamasını gerçekleştirin (Kalan Kota: ${bestWalletCam.remainingQuota.roundToInt()} ₺).",
                    inAppNavigationPath = bestWalletCam.whereToFindInApp,
                    earnedCashback = walletReward
                )
            ),
            involvedCampaigns = listOf(bestBankCam, bestWalletCam),
            warningNotes = notes
        )
    }

    private fun buildSplitSpendStrategy(
        spendAmount: Double,
        relevantCampaigns: List<Campaign>,
        excludedCount: Int
    ): StrategyPlan? {
        val eligible = relevantCampaigns
            .filter { it.minSpendAmount <= spendAmount && it.remainingQuota > 0.0 }
            .sortedByDescending { it.calculateCashbackForSpend(it.minSpendAmount) / it.minSpendAmount.coerceAtLeast(1.0) }

        if (eligible.size < 2) return null

        var remaining = spendAmount
        val steps = mutableListOf<StrategyStep>()
        val usedCampaigns = mutableListOf<Campaign>()
        var totalSplitCashback = 0.0

        for (cam in eligible) {
            if (remaining >= cam.minSpendAmount && steps.size < 3) {
                val optimalSlice = if (cam.cashbackRatePercent > 0.0) {
                    val maxEffectiveSpend = (cam.remainingQuota / (cam.cashbackRatePercent / 100.0))
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
                                actionTitle = "${optimalSlice.roundToInt()} ₺'lik Kısmı ${cam.appName} ile Öde",
                                actionDetail = "'${cam.title}' kampanyasının kalan ${cam.remainingQuota.roundToInt()} ₺ kotasından ${reward.roundToInt()} ₺ nakit iadeyi alın.",
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

        val notes = mutableListOf(
            "Kasada veya online alışverişte ödemeyi bölerek her uygulamanın kalan kotasını maksimum verimle kullanabilirsiniz."
        )
        if (excludedCount > 0) {
            notes.add("Kotası dolmuş $excludedCount kampanya hesaptan otomatik çıkarıldı.")
        }

        return StrategyPlan(
            id = "plan_split_${usedCampaigns.joinToString("_") { it.id }}",
            strategyType = StrategyType.SPLIT_SPEND,
            title = "Harcamayı ${steps.size} Parçaya Böl: +${extraGain.roundToInt()} ₺ Fazladan Nakit İade!",
            subtitle = "${spendAmount.roundToInt()} ₺'yi tek karttan çekerseniz kalan kota sınırı nedeniyle ${bestSingle.roundToInt()} ₺ alırsınız. Bölerek ${totalSplitCashback.roundToInt()} ₺ alın!",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = totalSplitCashback,
            effectiveReturnPercent = returnRate,
            steps = steps,
            involvedCampaigns = usedCampaigns,
            warningNotes = notes
        )
    }

    private fun buildSingleBestStrategy(
        spendAmount: Double,
        relevantCampaigns: List<Campaign>,
        excludedCount: Int
    ): StrategyPlan? {
        val bestPair = relevantCampaigns
            .map { cam -> cam to cam.calculateCashbackForSpend(spendAmount) }
            .filter { it.second > 0.0 }
            .maxByOrNull { it.second } ?: return null

        val (cam, reward) = bestPair
        val returnRate = ((reward / spendAmount) * 100.0).coerceAtMost(100.0)

        val notes = mutableListOf<String>()
        if (cam.requiresPreJoin) {
            notes.add("DİKKAT: Ödeme öncesi '${cam.whereToFindInApp}' menüsünden 'Hemen Katıl' yapılmalıdır.")
        }
        if (excludedCount > 0) {
            notes.add("Bu kategoride kotası dolan $excludedCount kampanya otomatik elendiği için sıradaki en yüksek iadeli kampanya seçildi.")
        }

        return StrategyPlan(
            id = "plan_single_${cam.id}",
            strategyType = StrategyType.SINGLE_OPTIMAL,
            title = "Tek İşlemde En Karlı Seçim: ${cam.appName}",
            subtitle = "Kalan ${cam.remainingQuota.roundToInt()} ₺ kotası üzerinden tek seferde en yüksek net iadeyi bu kampanya veriyor.",
            targetSpendAmount = spendAmount,
            totalCashbackEarned = reward,
            effectiveReturnPercent = returnRate,
            steps = listOf(
                StrategyStep(
                    stepNumber = 1,
                    appId = cam.appId,
                    appName = cam.appName,
                    actionTitle = if (cam.requiresPreJoin) "Önce '${cam.appName}' Uygulamasından Katıl, Sonra Öde" else "${cam.appName} ile Doğrudan Öde",
                    actionDetail = "${cam.description} (Kalan Kota: ${cam.remainingQuota.roundToInt()} ₺ • Bu işlemden kazanç: +${reward.roundToInt()} ₺).",
                    inAppNavigationPath = cam.whereToFindInApp,
                    earnedCashback = reward
                )
            ),
            involvedCampaigns = listOf(cam),
            warningNotes = notes
        )
    }
}
