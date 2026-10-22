package com.nakitradar.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.nakitradar.app.model.InstalledAppInfo

class InstalledAppScanner(private val context: Context) {

    /**
     * Cihazda yüklü olan operatör, cüzdan (Paycell, Nays, Papara vb.) ve banka uygulamalarını tespit eder.
     * Hem ana paket adını hem de bankaların mobil bankacılık eş paket adlarını kontrol eder.
     */
    fun scanDeviceApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val companionPackages = mapOf(
            "bonusflas" to listOf("com.garanti.bonusflas", "com.garanti.cepsubesi"),
            "juzdan" to listOf("com.akbank.android.apps.juzdan", "com.akbank.android.apps.akbank_direkt"),
            "worldmobil" to listOf("com.ykb.android.world", "com.ykb.android"),
            "maximum" to listOf("com.softtech.isbankasi.maximum", "com.pozitron.iscep"),
            "bankkart" to listOf("com.ziraat.bankkartmobil", "com.ziraat.ziraatmobil"),
            "qnb" to listOf("com.finansbank.mobile.cepsube", "com.finansbank.enpara")
        )

        return AppRegistry.supportedTurkishApps.map { app ->
            val packagesToCheck = companionPackages[app.id] ?: listOf(app.packageName)
            val installed = packagesToCheck.any { pkg -> isPackageInstalled(pm, pkg) }
            app.copy(
                isInstalledOnDevice = installed,
                isSelectedByUser = installed || app.isSelectedByUser
            )
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Kullanıcı strateji adımındaki "Uygulamayı Aç & Hemen Katıl" butonuna bastığında
     * doğrudan telefondaki ilgili uygulamayı (veya yüklü değilse resmi kampanya sayfasını) açar.
     */
    fun openAppOrCampaignPage(app: InstalledAppInfo) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.officialCampaignUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }
}
