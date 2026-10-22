package com.nakitradar.app.data

import com.nakitradar.app.model.AppEcosystemType
import com.nakitradar.app.model.InstalledAppInfo

object AppRegistry {

    val supportedTurkishApps: List<InstalledAppInfo> = listOf(
        // 1. TURKCELL & PAYCELL EKOSİSTEMİ
        InstalledAppInfo(
            id = "turkcell",
            name = "Turkcell Uygulaması",
            packageName = "com.ttech.android.onlineislem",
            ecosystemType = AppEcosystemType.OPERATOR_ECOSYSTEM,
            brandColorHex = 0xFFFFC900,
            shortBadge = "TCELL",
            officialCampaignUrl = "https://www.turkcell.com.tr/kampanyalar",
            hiddenCampaignPathHint = "1) Ayrıcalıklar > Kampanyalar  2) Pasaj > Kampanyalar  3) Fatura > Paycell Ödeme Fırsatı  4) Hediye Havuzu",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "paycell",
            name = "Paycell",
            packageName = "com.turkcell.paycell",
            ecosystemType = AppEcosystemType.PREPAID_WALLET,
            brandColorHex = 0xFF0057B8,
            shortBadge = "PAYCELL",
            officialCampaignUrl = "https://paycell.com.tr/kampanyalar",
            hiddenCampaignPathHint = "1) Fırsatlar > Nakit İade (Hemen Katıl!)  2) Hediye Dünyası > Puanları Nakite Çevir  3) Fatura Öde > Fatura İadeleri",
            supportsCreditCardTopUpCombo = true,
            isSelectedByUser = true
        ),

        // 2. ÖN ÖDEMELİ NAKİT İADE CÜZDANLARI (KOMBO / STACKING ARAÇLARI)
        InstalledAppInfo(
            id = "nays",
            name = "Nays (İş Bankası)",
            packageName = "com.isbank.nays",
            ecosystemType = AppEcosystemType.PREPAID_WALLET,
            brandColorHex = 0xFF7B2CBF,
            shortBadge = "NAYS",
            officialCampaignUrl = "https://www.naysapp.com.tr/firsatlar",
            hiddenCampaignPathHint = "Ana Sayfa > Fırsatlar > E-Ticaret / Market / Fatura Hediye Nakit + Harca Kazan (Şimdi Kazan)",
            supportsCreditCardTopUpCombo = true,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "papara",
            name = "Papara",
            packageName = "com.mobillium.papara",
            ecosystemType = AppEcosystemType.PREPAID_WALLET,
            brandColorHex = 0xFF111827,
            shortBadge = "PAPARA",
            officialCampaignUrl = "https://www.papara.com/cashback",
            hiddenCampaignPathHint = "Papara Card > Cashback > Kategori Bazlı Anında Nakit İade (Katılım Gerekmez, Otomatik Yatar)",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "tosla",
            name = "Tosla (Akbank)",
            packageName = "com.akode.tosla",
            ecosystemType = AppEcosystemType.PREPAID_WALLET,
            brandColorHex = 0xFFFF3366,
            shortBadge = "TOSLA",
            officialCampaignUrl = "https://tosla.com/kampanyalar",
            hiddenCampaignPathHint = "Profil > Kampanyalar > Tosback Fırsatları (Katıl Butonu Zorunlu Olanları Kontrol Edin)",
            supportsCreditCardTopUpCombo = true,
            isSelectedByUser = false
        ),
        InstalledAppInfo(
            id = "hadi",
            name = "Hadi (TOM Bank / A101)",
            packageName = "com.tombank.hadi",
            ecosystemType = AppEcosystemType.PREPAID_WALLET,
            brandColorHex = 0xFF00B4D8,
            shortBadge = "HADİ",
            officialCampaignUrl = "https://haditombank.com/kampanyalar",
            hiddenCampaignPathHint = "Kampanyalar > A101 & Market Nakit İade / Gold Üyelik Nakit İadeleri",
            supportsCreditCardTopUpCombo = true,
            isSelectedByUser = false
        ),
        InstalledAppInfo(
            id = "pokus",
            name = "Pokus (Türk Telekom)",
            packageName = "com.turktelekom.pokus",
            ecosystemType = AppEcosystemType.PREPAID_WALLET,
            brandColorHex = 0xFF00A896,
            shortBadge = "POKUS",
            officialCampaignUrl = "https://pokus.com.tr/kampanyalar",
            hiddenCampaignPathHint = "Kampanyalar > Nakit İade > Harcamadan Önce Katıl",
            supportsCreditCardTopUpCombo = true,
            isSelectedByUser = false
        ),

        // 3. BANKA VE KREDİ KARTI KAMPANYA UYGULAMALARI
        InstalledAppInfo(
            id = "bonusflas",
            name = "Garanti BBVA & BonusFlaş",
            packageName = "com.garanti.bonusflas",
            ecosystemType = AppEcosystemType.BANK_CAMPAIGN_APP,
            brandColorHex = 0xFF16A34A,
            shortBadge = "BONUS",
            officialCampaignUrl = "https://www.bonus.com.tr/kampanyalar",
            hiddenCampaignPathHint = "1) Bana Özel (Gizli Ekstra Bonus!)  2) Süpermarket/Akaryakıt/E-Ticaret > Hemen Katıl  3) GarantiPay Ekstra İade",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "juzdan",
            name = "Akbank & Juzdan",
            packageName = "com.akbank.android.apps.juzdan",
            ecosystemType = AppEcosystemType.BANK_CAMPAIGN_APP,
            brandColorHex = 0xFFDC2626,
            shortBadge = "JUZDAN",
            officialCampaignUrl = "https://www.axess.com.tr/axess/kampanya",
            hiddenCampaignPathHint = "1) Senin İçin > Bana Özel Chip-Para  2) Kampanyalar > Hemen Katıl  3) Chip-Para ile Öde / Fatura İadesi",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "worldmobil",
            name = "Yapı Kredi & World Mobil",
            packageName = "com.ykb.android.world",
            ecosystemType = AppEcosystemType.BANK_CAMPAIGN_APP,
            brandColorHex = 0xFF6D28D9,
            shortBadge = "WORLD",
            officialCampaignUrl = "https://www.worldcard.com.tr/kampanyalar",
            hiddenCampaignPathHint = "1) Sana Özel > Kişisel Hedef Kampanyası  2) World Pay Ekstra Puan  3) Sektör Kampanyaları > Katıl",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "maximum",
            name = "İşCep & Maximum Mobil",
            packageName = "com.softtech.isbankasi.maximum",
            ecosystemType = AppEcosystemType.BANK_CAMPAIGN_APP,
            brandColorHex = 0xFF1D4ED8,
            shortBadge = "MAXI",
            officialCampaignUrl = "https://www.maximum.com.tr/kampanyalar",
            hiddenCampaignPathHint = "1) Kampanyalar > Bana Özel  2) Pazarama / QR Ödeme Ekstra MaxiPuan  3) Katıldıklarım Sekmesi",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "bankkart",
            name = "Ziraat & Bankkart Mobil",
            packageName = "com.ziraat.bankkartmobil",
            ecosystemType = AppEcosystemType.BANK_CAMPAIGN_APP,
            brandColorHex = 0xFFB91C1C,
            shortBadge = "ZİRAAT",
            officialCampaignUrl = "https://www.bankkart.com.tr/kampanyalar",
            hiddenCampaignPathHint = "Kampanyalar > Sektör Seçimi > Hemen Katıl (Bankkart Lira ile Ekstre İndirimi Yapılabilir)",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = true
        ),
        InstalledAppInfo(
            id = "qnb",
            name = "QNB Mobil & Enpara",
            packageName = "com.finansbank.mobile.cepsube",
            ecosystemType = AppEcosystemType.BANK_CAMPAIGN_APP,
            brandColorHex = 0xFF0F172A,
            shortBadge = "QNB",
            officialCampaignUrl = "https://www.cardfinans.com/kampanyalar",
            hiddenCampaignPathHint = "Kampanyalar > Harcama Sözüne / Sektörel ParaPuan & Enpara Ayın Enparalısı İadeleri",
            supportsCreditCardTopUpCombo = false,
            isSelectedByUser = false
        )
    )

    fun findByPackageName(packageName: String): InstalledAppInfo? {
        val aliasMap = mapOf(
            "com.garanti.cepsubesi" to "bonusflas",
            "com.akbank.android.apps.akbank_direkt" to "juzdan",
            "com.ykb.android" to "worldmobil",
            "com.pozitron.iscep" to "maximum",
            "com.ziraat.ziraatmobil" to "bankkart",
            "com.finansbank.enpara" to "qnb"
        )
        val mappedId = aliasMap[packageName]
        return supportedTurkishApps.find {
            it.packageName == packageName || (mappedId != null && it.id == mappedId)
        }
    }
}
