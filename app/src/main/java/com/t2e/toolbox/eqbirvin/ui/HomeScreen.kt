package com.t2e.toolbox.eqbirvin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDisplayFix: () -> Unit,
    onNavigateToUnifiedPullDown: () -> Unit,
    onNavigateToVolumeMode: () -> Unit,
    onNavigateToFlashlightIndicator: () -> Unit,
    onNavigateToPerAppDpi: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("T2E Toolbox") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPerAppDpi() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Per-App Scaling (DPI)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Force tablet or desktop layouts for specific apps (e.g. Instagram) to eliminate content cutoff.")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDisplayFix() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Fix", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Adjust rendering resolution and density via Shizuku.")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToUnifiedPullDown() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Unified Pull-Down", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Stock-style status bar swipe (notifications, then Quick Settings).")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToVolumeMode() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Volume Mode Switcher", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Side key shortcut to cycle between Sound, Vibrate, and Silent modes.")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFlashlightIndicator() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Flashlight Indicator", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Displays a clean horizontal icon in the center of the status bar when the flashlight is active.")
                }
            }
        }
    }
}
