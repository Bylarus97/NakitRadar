package com.nakitradar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nakitradar.app.data.InstalledAppScanner
import com.nakitradar.app.data.LocalQuotaStorage
import com.nakitradar.app.ui.NakitRadarApp
import com.nakitradar.app.ui.NakitRadarViewModel
import com.nakitradar.app.ui.theme.NakitRadarTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NakitRadarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val storage = LocalQuotaStorage(applicationContext)
        viewModel.initializeStorage(storage)

        val scanner = InstalledAppScanner(applicationContext)
        viewModel.scanDeviceApps(scanner)

        // Başka bir uygulamadan "Paylaş -> NakitRadar" ile kampanya metni gönderildiyse otomatik ekle
        handleIncomingSharedText(intent)

        setContent {
            NakitRadarTheme {
                NakitRadarApp(
                    viewModel = viewModel,
                    scanner = scanner
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingSharedText(intent)
    }

    private fun handleIncomingSharedText(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val lower = sharedText.lowercase()
                val guessedAppId = when {
                    lower.contains("paycell") -> "paycell"
                    lower.contains("turkcell") || lower.contains("pasaj") -> "turkcell"
                    lower.contains("nays") -> "nays"
                    lower.contains("papara") -> "papara"
                    lower.contains("bonus") || lower.contains("garanti") -> "bonusflas"
                    lower.contains("juzdan") || lower.contains("axess") || lower.contains("chip") -> "juzdan"
                    lower.contains("world") || lower.contains("yapı kredi") -> "worldmobil"
                    lower.contains("maximum") || lower.contains("maxipuan") -> "maximum"
                    lower.contains("bankkart") || lower.contains("ziraat") -> "bankkart"
                    else -> "paycell"
                }
                viewModel.importCampaignFromText(sharedText, guessedAppId)
            }
        }
    }
}
