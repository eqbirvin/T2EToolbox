package com.t2e.toolbox.eqbirvin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.t2e.toolbox.eqbirvin.ui.AppViewModelFactory
import com.t2e.toolbox.eqbirvin.ui.HomeScreen
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixScreen
import com.t2e.toolbox.eqbirvin.ui.displayfix.DisplayFixViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as T2EApplication
        val settingsRepo = app.settingsRepository
        val displayRepo = app.displayFixRepository

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
                                onRequestPermission = { viewModel.onRequestPermission() },
                                onRefresh = { viewModel.refreshState() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
