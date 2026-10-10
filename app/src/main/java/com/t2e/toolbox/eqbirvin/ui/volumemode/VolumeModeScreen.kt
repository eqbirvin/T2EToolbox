package com.t2e.toolbox.eqbirvin.ui.volumemode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.t2e.toolbox.eqbirvin.data.RingerModeState
import com.t2e.toolbox.eqbirvin.data.VolumeCycleOption

data class VolumeModeActions(
    val onToggleShortcut: (Boolean) -> Unit,
    val onToggleToast: (Boolean) -> Unit,
    val onToggleHaptic: (Boolean) -> Unit,
    val onSetCycleOption: (VolumeCycleOption) -> Unit,
    val onCycleMode: () -> Unit,
    val onSetSpecificMode: (RingerModeState) -> Unit,
    val onGrantDndViaShizuku: () -> Unit,
    val onOpenDndSettings: () -> Unit,
    val onRefresh: () -> Unit,
    val onBack: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeModeScreen(
    uiState: VolumeModeUiState,
    actions: VolumeModeActions
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volume Mode Switcher") },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
            // Main Feature Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isShortcutEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = "Enable Volume Mode Shortcut",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Adds 'Volume Mode' app entry to the launcher so the Titan 2 Elite programmable side key can launch it.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = uiState.isShortcutEnabled,
                        onCheckedChange = actions.onToggleShortcut
                    )
                }
            }

            // Current Mode & Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Current Status", style = MaterialTheme.typography.titleMedium)

                    // Current Ringer Mode Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Sound Mode:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (prefix, badgeColor) = when (uiState.currentRingerMode) {
                                RingerModeState.NORMAL -> "🔊" to MaterialTheme.colorScheme.primary
                                RingerModeState.VIBRATE -> "📳" to MaterialTheme.colorScheme.tertiary
                                RingerModeState.SILENT -> "🔇" to MaterialTheme.colorScheme.error
                            }
                            Text(prefix, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.currentRingerMode.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = badgeColor
                            )
                        }
                    }

                    HorizontalDivider()

                    // DND Permission Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Do Not Disturb Access:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isDndAccessGranted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Granted", color = Color(0xFF2E7D32))
                            } else {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Missing", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (!uiState.isDndAccessGranted) {
                        Text(
                            text = "Android requires Do Not Disturb permission to cycle into or out of Silent mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isShizukuReady) {
                                Button(
                                    onClick = actions.onGrantDndViaShizuku,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Grant with Shizuku")
                                }
                            }
                            OutlinedButton(
                                onClick = actions.onOpenDndSettings,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("System Settings")
                            }
                        }
                    }
                }
            }

            // Behavior & Feedback Preferences Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Feedback & Behavior", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Toast on Switch")
                            Text("Displays a brief overlay with the active mode name.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = uiState.isToastEnabled,
                            onCheckedChange = actions.onToggleToast
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Feedback on Switch")
                            Text("Vibration cues tailored to the newly selected mode.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = uiState.isHapticEnabled,
                            onCheckedChange = actions.onToggleHaptic
                        )
                    }

                    HorizontalDivider()

                    Text("Cycle Sequence", style = MaterialTheme.typography.bodyMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val options = listOf(
                            VolumeCycleOption.ALL_MODES to "Sound → Vibrate → Silent (Default)",
                            VolumeCycleOption.SOUND_AND_VIBRATE to "Sound ↔ Vibrate",
                            VolumeCycleOption.SOUND_AND_SILENT to "Sound ↔ Silent",
                            VolumeCycleOption.VIBRATE_AND_SILENT to "Vibrate ↔ Silent"
                        )
                        options.forEach { (option, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.cycleOption == option,
                                    onClick = { actions.onSetCycleOption(option) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Interactive Testing Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Interactive Testing", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = actions.onCycleMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Cycle Mode (Next)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { actions.onSetSpecificMode(RingerModeState.NORMAL) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sound")
                        }
                        OutlinedButton(
                            onClick = { actions.onSetSpecificMode(RingerModeState.VIBRATE) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Vibrate")
                        }
                        OutlinedButton(
                            onClick = { actions.onSetSpecificMode(RingerModeState.SILENT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Silent")
                        }
                    }

                    uiState.lastActionMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Setup Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Titan 2 Elite Setup Guide", style = MaterialTheme.typography.titleMedium)
                    Text("1. Turn ON 'Enable Volume Mode Shortcut' above.", style = MaterialTheme.typography.bodySmall)
                    Text("2. Open device Settings → Programmable Key (or Assistant Key).", style = MaterialTheme.typography.bodySmall)
                    Text("3. Choose Short press, Long press, or Double press → Launch app.", style = MaterialTheme.typography.bodySmall)
                    Text("4. Select 'Volume Mode' from the list of installed apps.", style = MaterialTheme.typography.bodySmall)
                    Text("5. Press the red physical side key to effortlessly cycle sound modes!", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
