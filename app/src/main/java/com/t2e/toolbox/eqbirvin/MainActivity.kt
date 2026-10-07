package com.t2e.toolbox.eqbirvin

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.ui.AppViewModelFactory
import com.t2e.toolbox.eqbirvin.ui.HomeScreen
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixScreen
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixViewModel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var displayRepo: DisplayFixRepository
    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        // Could refresh state here if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepo = SettingsRepository(applicationContext)
        displayRepo = DisplayFixRepository(settingsRepo)

        try {
            Shizuku.addRequestPermissionResultListener(shizukuListener)
        } catch (e: Exception) {
            // Shizuku API not available or error
        }

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
                                onNavigateToDisplayFix = { navController.navigate("displayFix") }
                            )
                        }
                        composable("displayFix") {
                            val viewModel: DisplayFixViewModel = viewModel(
                                factory = AppViewModelFactory(settingsRepo, displayRepo)
                            )
                            val uiState by viewModel.uiState.collectAsState()

                            DisplayFixScreen(
                                uiState = uiState,
                                onToggle = { enabled -> viewModel.toggleFix(enabled) },
                                onRunNow = { viewModel.runNow() },
                                onRefresh = { viewModel.refreshState() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuListener)
        } catch (e: Exception) {}
    }
}
