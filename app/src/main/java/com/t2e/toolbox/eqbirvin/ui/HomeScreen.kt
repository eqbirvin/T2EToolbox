package com.t2e.toolbox.eqbirvin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.t2e.toolbox.eqbirvin.BuildConfig

data class HomeScreenActions(
    val onNavigateToKeyboardBacklight: () -> Unit,
    val onNavigateToTypingScrollBlock: () -> Unit,
    val onNavigateToPerAppDpi: () -> Unit,
    val onNavigateToDisplayFix: () -> Unit,
    val onNavigateToUnifiedPullDown: () -> Unit,
    val onNavigateToVolumeMode: () -> Unit,
    val onNavigateToFlashlightIndicator: () -> Unit,
    val onNavigateToCtsCleanup: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(actions: HomeScreenActions) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("T2E Toolbox")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            })
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
                    .clickable { actions.onNavigateToKeyboardBacklight() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Keyboard Backlight", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Custom timeout up to 5 minutes, keep lit while screen is on, and brightness control.")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.onNavigateToTypingScrollBlock() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Block Scroll While Typing", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Prevent accidental trackpad scrolls and page bouncing while actively typing on the physical keyboard.")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.onNavigateToPerAppDpi() }
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
                    .clickable { actions.onNavigateToDisplayFix() }
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
                    .clickable { actions.onNavigateToUnifiedPullDown() }
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
                    .clickable { actions.onNavigateToVolumeMode() }
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
                    .clickable { actions.onNavigateToFlashlightIndicator() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Flashlight Indicator", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Displays a clean horizontal icon in the center of the status bar when the flashlight is active.")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.onNavigateToCtsCleanup() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Circle to Search Cleanup", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Auto-delete orphaned screenshots after Google Lens opens, and one-tap storage purge.")
                }
            }
        }
    }
}
