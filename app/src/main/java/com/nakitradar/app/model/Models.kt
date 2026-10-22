package com.nakitradar.app.model

enum class AppEcosystemType(val title: String) {
    OPERATOR_ECOSYSTEM("Operatör & Süper Uygulama"),
    PREPAID_WALLET("Nakit İade Cüzdanı (Kombo Araçları)"),
    BANK_CAMPAIGN_APP("Banka & Kart Kampanya Uygulaması")
}

enum class SpendCategory(val displayName: String, val iconEmoji: String) {
    FATURA_OPERATOR("Fatura & Operatör (Turkcell vb.)", "📱"),
    MARKET_GIDA("Market & Süpermarket", "🛒"),
    AKARYAKIT("Akaryakıt & Otogaz", "⛽"),
    ETICARET("E-Ticaret & Online Alışveriş", "📦"),
    RESTORAN_KAFE("Restoran, Kafe & Yemek", "🍔"),
    DIJITAL_OYUN("Dijital Abonelik & Oyun", "🎮"),
    ULASIM("Ulaşım & İstanbulkart", "🚇"),
    ECZANE_SAGLIK("Eczane & Sağlık", "💊"),
    GENEL("Genel / Tüm Harcamalar", "💳")
}

enum class CashbackType(val label: String) {
    DIRECT_CASHBACK("Doğrudan Nakit İade"),
    REDEEMABLE_POINTS("Nakit Değerinde Puan (Nakit/Fatura İadesi)"),
    BILL_DISCOUNT("Fatura İndirimi / İadesi")
}

enum class CampaignSource(val badge: String) {
    OFFICIAL_WEB("Resmi Kampanya"),
    IN_APP_HIDDEN("Uygulama İçi Gizli Sekme"),
    NOTIFICATION_CAPTURED("Bildirimden Yakalandı"),
    AI_IMPORTED("AI / Ekran Görüntüsü")
}

data class InstalledAppInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val ecosystemType: AppEcosystemType,
    val brandColorHex: Long,
    val shortBadge: String,
    val officialCampaignUrl: String,
    val hiddenCampaignPathHint: String,
    val supportsCreditCardTopUpCombo: Boolean = false,
    val isInstalledOnDevice: Boolean = false,
    val isSelectedByUser: Boolean = true
)

data class Campaign(
    val id: String,
    val appId: String,
    val appName: String,
    val title: String,
    val description: String,
    val whereToFindInApp: String,
    val category: SpendCategory,
    val cashbackType: CashbackType,
    val minSpendAmount: Double,
    val requiredTransactionCount: Int = 1,
    val cashbackRatePercent: Double = 0.0,
    val flatCashbackAmount: Double = 0.0,
    val maxCashbackCap: Double,
    val requiresPreJoin: Boolean = true,
    val canStackAsWalletTopUp: Boolean = false,
    val acceptsPrepaidWalletPayment: Boolean = true,
    val synergyPartnerAppId: String? = null,
    val synergyBonusAmount: Double = 0.0,
    val synergyExplanation: String? = null,
    val validUntil: String,
    val source: CampaignSource = CampaignSource.OFFICIAL_WEB,
    val isJoined: Boolean = false,
    val completedSteps: Int = 0
) {
    /**
     * Belirli bir harcama tutarı için bu kampanyadan tek başına alınabilecek net nakit iadeyi hesaplar.
     */
    fun calculateCashbackForSpend(spendAmount: Double): Double {
        val totalRequiredMin = minSpendAmount * requiredTransactionCount
        if (spendAmount < totalRequiredMin) return 0.0

        val rawReward = if (flatCashbackAmount > 0.0 && cashbackRatePercent == 0.0) {
            flatCashbackAmount
        } else if (cashbackRatePercent > 0.0) {
            (spendAmount * (cashbackRatePercent / 100.0)) + flatCashbackAmount
        } else {
            0.0
        }
        return rawReward.coerceAtMost(maxCashbackCap)
    }
}

enum class StrategyType(val badge: String) {
    COMBO_STACKING("🔥 ÇİFTE KAZANÇ KOMBOSU (STACKING)"),
    ECOSYSTEM_SYNERGY("⚡ TURKCELL + PAYCELL + BANKA ZİNCİRİ"),
    SPLIT_SPEND("✂️ HARCAMA BÖLME (ÇOKLU İADE) STRATEJİSİ"),
    SINGLE_OPTIMAL("🎯 TEK İŞLEMDE MAKSİMUM İADE")
}

data class StrategyStep(
    val stepNumber: Int,
    val appId: String,
    val appName: String,
    val actionTitle: String,
    val actionDetail: String,
    val inAppNavigationPath: String,
    val earnedCashback: Double
)

data class StrategyPlan(
    val id: String,
    val strategyType: StrategyType,
    val title: String,
    val subtitle: String,
    val targetSpendAmount: Double,
    val totalCashbackEarned: Double,
    val effectiveReturnPercent: Double,
    val steps: List<StrategyStep>,
    val involvedCampaigns: List<Campaign>,
    val warningNotes: List<String>
)
