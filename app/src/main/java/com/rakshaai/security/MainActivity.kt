package com.rakshaai.security

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.data.preference.LanguageManager
import com.rakshaai.security.data.repository.ScanRepository
import com.rakshaai.security.domain.engine.LocalDetectionEngine
import com.rakshaai.security.ui.analysis.AnalysisScreen
import com.rakshaai.security.ui.awareness.CyberAwarenessScreen
import com.rakshaai.security.ui.components.BottomNavBar
import com.rakshaai.security.ui.components.NavDestination
import com.rakshaai.security.ui.dashboard.DashboardScreen
import com.rakshaai.security.ui.history.HistoryScreen
import com.rakshaai.security.ui.language.LanguageSelectionScreen
import com.rakshaai.security.ui.privacy.PrivacySettingsScreen
import com.rakshaai.security.ui.result.ResultScreen
import com.rakshaai.security.ui.scan.ScanScreen
import com.rakshaai.security.ui.theme.RakshaAITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class ScreenState {
    data object LanguageSelect : ScreenState()
    data object Dashboard : ScreenState()
    data class InputScan(val initialType: ContentType = ContentType.MESSAGE) : ScreenState()
    data class Analyzing(val contentType: ContentType, val text: String, val imageUri: String? = null) : ScreenState()
    data class Result(val scanResult: ScanResult) : ScreenState()
    data object History : ScreenState()
    data object Awareness : ScreenState()
    data object Privacy : ScreenState()
}

class MainActivity : ComponentActivity() {
    private var pendingScanId = mutableStateOf<String?>(null)
    private var sharedScanText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingScanId.value = intent?.getStringExtra("scan_id")
        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            sharedScanText.value = intent?.getStringExtra(Intent.EXTRA_TEXT)
        }

        ScanRepository.initialize(applicationContext)
        com.rakshaai.security.data.network.OnlineMonitoringManager.initialize(applicationContext)

        // Initialize ML classifier on background thread
        lifecycleScope.launch(Dispatchers.IO) {
            LocalDetectionEngine.initMlClassifier(applicationContext)
        }

        enableEdgeToEdge()

        val isLanguageSelected = LanguageManager.isLanguageSelected(this)
        val activity = this

        setContent {
            var currentLang by remember { mutableStateOf(LanguageManager.getLanguage(activity)) }
            val localizedContext = remember(currentLang) {
                LanguageManager.getLocalizedContext(activity)
            }

            val initialStartScreen = when {
                !isLanguageSelected -> ScreenState.LanguageSelect
                !sharedScanText.value.isNullOrBlank() -> ScreenState.Analyzing(ContentType.URL, sharedScanText.value!!)
                else -> ScreenState.Dashboard
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides activity
            ) {
                RakshaAITheme {
                    RakshaMainApp(
                        initialScreen = initialStartScreen,
                        initialScanId = pendingScanId.value,
                        onLanguageChange = { newLang ->
                            currentLang = newLang
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val scanId = intent?.getStringExtra("scan_id")
        if (!scanId.isNullOrEmpty()) {
            pendingScanId.value = scanId
        }
        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                sharedScanText.value = text
            }
        }
    }
}

@Composable
fun RakshaMainApp(
    initialScreen: ScreenState,
    initialScanId: String?,
    onLanguageChange: (String) -> Unit
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    val context = LocalContext.current
    val history by ScanRepository.history.collectAsState()

    androidx.compose.runtime.LaunchedEffect(initialScanId, history) {
        if (!initialScanId.isNullOrEmpty()) {
            val matched = history.find { it.id == initialScanId }
            if (matched != null) {
                currentScreen = ScreenState.Result(matched)
            }
        }
    }

    if (currentScreen is ScreenState.LanguageSelect) {
        LanguageSelectionScreen(
            onLanguageSelected = { langCode ->
                LanguageManager.setLanguage(context, langCode)
                onLanguageChange(langCode)
                currentScreen = ScreenState.Dashboard
            }
        )
        return
    }

    val activeNavDestination = when (currentScreen) {
        is ScreenState.Dashboard -> NavDestination.HOME
        is ScreenState.InputScan, is ScreenState.Analyzing -> NavDestination.SCAN
        is ScreenState.Result -> NavDestination.SCAN
        is ScreenState.History -> NavDestination.HISTORY
        is ScreenState.Awareness -> NavDestination.AWARENESS
        is ScreenState.Privacy -> NavDestination.PRIVACY
        else -> NavDestination.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentDestination = activeNavDestination,
                onNavigate = { destination ->
                    currentScreen = when (destination) {
                        NavDestination.HOME -> ScreenState.Dashboard
                        NavDestination.SCAN -> ScreenState.InputScan()
                        NavDestination.HISTORY -> ScreenState.History
                        NavDestination.AWARENESS -> ScreenState.Awareness
                        NavDestination.PRIVACY -> ScreenState.Privacy
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is ScreenState.LanguageSelect -> {}

                is ScreenState.Dashboard -> {
                    DashboardScreen(
                        onNavigateToScan = { type ->
                            currentScreen = ScreenState.InputScan(type)
                        },
                        onNavigateToResult = { result ->
                            currentScreen = ScreenState.Result(result)
                        },
                        onNavigateToHistory = {
                            currentScreen = ScreenState.History
                        }
                    )
                }

                is ScreenState.InputScan -> {
                    ScanScreen(
                        initialContentType = screen.initialType,
                        onStartScan = { type, text, imageUri ->
                            currentScreen = ScreenState.Analyzing(type, text, imageUri)
                        }
                    )
                }

                is ScreenState.Analyzing -> {
                    AnalysisScreen(
                        contentType = screen.contentType,
                        rawContent = screen.text,
                        imageUri = screen.imageUri,
                        onAnalysisComplete = { result ->
                            currentScreen = ScreenState.Result(result)
                        }
                    )
                }

                is ScreenState.Result -> {
                    ResultScreen(
                        scanResult = screen.scanResult,
                        onNavigateBack = {
                            currentScreen = ScreenState.Dashboard
                        },
                        onScanAnother = {
                            currentScreen = ScreenState.InputScan()
                        }
                    )
                }

                is ScreenState.History -> {
                    HistoryScreen(
                        onSelectScanResult = { result ->
                            currentScreen = ScreenState.Result(result)
                        }
                    )
                }

                is ScreenState.Awareness -> {
                    CyberAwarenessScreen(
                        onNavigateToScan = {
                            currentScreen = ScreenState.InputScan()
                        }
                    )
                }

                is ScreenState.Privacy -> {
                    PrivacySettingsScreen(
                        onLanguageChanged = { newLang ->
                            onLanguageChange(newLang)
                        }
                    )
                }
            }
        }
    }
}
