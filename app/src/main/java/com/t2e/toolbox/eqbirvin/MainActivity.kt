package com.t2e.toolbox.eqbirvin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.t2e.toolbox.eqbirvin.ui.AppViewModelFactory
import com.t2e.toolbox.eqbirvin.ui.HomeScreen
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixScreen
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixViewModel
import com.t2e.toolbox.eqbirvin.ui.flashlight.FlashlightIndicatorActions
import com.t2e.toolbox.eqbirvin.ui.flashlight.FlashlightIndicatorScreen
import com.t2e.toolbox.eqbirvin.ui.flashlight.FlashlightIndicatorViewModel
import com.t2e.toolbox.eqbirvin.ui.perappdpi.PerAppDpiActions
import com.t2e.toolbox.eqbirvin.ui.perappdpi.PerAppDpiScreen
import com.t2e.toolbox.eqbirvin.ui.perappdpi.PerAppDpiViewModel
import com.t2e.toolbox.eqbirvin.ui.unifiedpulldown.UnifiedPullDownActions
import com.t2e.toolbox.eqbirvin.ui.unifiedpulldown.UnifiedPullDownScreen
import com.t2e.toolbox.eqbirvin.ui.unifiedpulldown.UnifiedPullDownViewModel
import com.t2e.toolbox.eqbirvin.ui.volumemode.VolumeModeActions
import com.t2e.toolbox.eqbirvin.ui.volumemode.VolumeModeScreen
import com.t2e.toolbox.eqbirvin.ui.volumemode.VolumeModeViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val app = application as T2EApplication
        val settingsRepo = app.settingsRepository
        val displayRepo = app.displayFixRepository
        val pullDownRepo = app.unifiedPullDownRepository
        val volumeRepo = app.volumeModeRepository
        val flashlightRepo = app.flashlightRepository
        val perAppDpiRepo = app.perAppDpiRepository

        val viewModelFactory = AppViewModelFactory(
            context = this,
            settingsRepo = settingsRepo,
            displayRepo = displayRepo,
            pullDownRepo = pullDownRepo,
            volumeRepo = volumeRepo,
            flashlightRepo = flashlightRepo,
            perAppDpiRepo = perAppDpiRepo
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToDisplayFix = { navController.navigate("displayFix") },
                                onNavigateToUnifiedPullDown = { navController.navigate("unifiedPullDown") },
                                onNavigateToVolumeMode = { navController.navigate("volumeMode") },
                                onNavigateToFlashlightIndicator = { navController.navigate("flashlightIndicator") },
                                onNavigateToPerAppDpi = { navController.navigate("perAppDpi") }
                            )
                        }
                        composable("perAppDpi") {
                            val viewModel: PerAppDpiViewModel = viewModel(factory = viewModelFactory)
                            val uiState by viewModel.uiState.collectAsState()

                            val actions = remember(viewModel, navController) {
                                PerAppDpiActions(
                                    onToggleEnabled = { viewModel.toggleEnabled(it) },
                                    onSetBaseDpi = { viewModel.setUserBaseDpi(it) },
                                    onSaveRule = { viewModel.saveRule(it) },
                                    onDeleteRule = { viewModel.deleteRule(it) },
                                    onOpenAppPicker = { viewModel.openAppPicker() },
                                    onCloseAppPicker = { viewModel.closeAppPicker() },
                                    onOpenRuleEditor = { viewModel.openRuleEditor(it) },
                                    onCloseRuleEditor = { viewModel.closeRuleEditor() },
                                    onRequestPermission = { viewModel.onRequestPermission() },
                                    onRefresh = { viewModel.refreshState() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            PerAppDpiScreen(
                                uiState = uiState,
                                actions = actions
                            )
                        }
                        composable("displayFix") {
                            val viewModel: DisplayFixViewModel = viewModel(factory = viewModelFactory)
                            val uiState by viewModel.uiState.collectAsState()

                            DisplayFixScreen(
                                uiState = uiState,
                                onToggle = { enabled -> viewModel.toggleFix(enabled) },
                                onRunNow = { viewModel.runNow() },
                                onRequestPermission = { viewModel.onRequestPermission() },
                                onRefresh = { viewModel.refreshState() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("unifiedPullDown") {
                            val viewModel: UnifiedPullDownViewModel = viewModel(factory = viewModelFactory)
                            val uiState by viewModel.uiState.collectAsState()

                            val actions = remember(viewModel, navController) {
                                UnifiedPullDownActions(
                                    onToggleEnabled = { viewModel.togglePullDownEnabled(it) },
                                    onToggleLockscreen = { viewModel.toggleLockscreen(it) },
                                    onToggleDebugTint = { viewModel.toggleDebugTint(it) },
                                    onToggleSmartShadeDetection = { viewModel.toggleSmartShadeDetection(it) },
                                    onToggleHapticFeedback = { viewModel.toggleHapticFeedback(it) },
                                    onSelectMethod = { viewModel.setMethod(it) },
                                    onEnableWithShizuku = { viewModel.enableWithShizuku() },
                                    onDisableWithShizuku = { viewModel.disableWithShizuku() },
                                    onOpenAccessibilitySettings = {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    },
                                    onTestNotifications = { viewModel.testOpenNotifications() },
                                    onTestQuickSettings = { viewModel.testOpenQuickSettings() },
                                    onRefresh = { viewModel.refreshState() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            UnifiedPullDownScreen(
                                uiState = uiState,
                                actions = actions
                            )
                        }
                        composable("volumeMode") {
                            val viewModel: VolumeModeViewModel = viewModel(factory = viewModelFactory)
                            val uiState by viewModel.uiState.collectAsState()

                            val actions = remember(viewModel, navController) {
                                VolumeModeActions(
                                    onToggleShortcut = { viewModel.toggleShortcut(it) },
                                    onToggleToast = { viewModel.toggleToast(it) },
                                    onToggleHaptic = { viewModel.toggleHaptic(it) },
                                    onSetCycleOption = { viewModel.setCycleOption(it) },
                                    onSetSilentModeBehavior = { viewModel.setSilentModeBehavior(it) },
                                    onCycleMode = { viewModel.cycleMode() },
                                    onSetSpecificMode = { viewModel.setSpecificMode(it) },
                                    onGrantDndViaShizuku = { viewModel.grantDndViaShizuku() },
                                    onOpenDndSettings = {
                                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                    },
                                    onRefresh = { viewModel.refreshState() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            VolumeModeScreen(
                                uiState = uiState,
                                actions = actions
                            )
                        }
                        composable("flashlightIndicator") {
                            val viewModel: FlashlightIndicatorViewModel = viewModel(factory = viewModelFactory)
                            val uiState by viewModel.uiState.collectAsState()

                            val actions = remember(viewModel, navController) {
                                FlashlightIndicatorActions(
                                    onToggleEnabled = { viewModel.toggleIndicatorEnabled(it) },
                                    onToggleTapToDismiss = { viewModel.toggleTapToDismiss(it) },
                                    onSetColor = { viewModel.setColor(it) },
                                    onToggleFlashlight = { viewModel.toggleFlashlight() },
                                    onEnableServiceWithShizuku = { viewModel.enableServiceWithShizuku() },
                                    onOpenAccessibilitySettings = {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            FlashlightIndicatorScreen(
                                uiState = uiState,
                                actions = actions
                            )
                        }
                    }
                }
            }
        }
    }
}
