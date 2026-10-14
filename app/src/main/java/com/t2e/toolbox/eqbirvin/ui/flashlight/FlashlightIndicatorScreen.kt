package com.t2e.toolbox.eqbirvin.ui.flashlight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.t2e.toolbox.eqbirvin.R
import com.t2e.toolbox.eqbirvin.data.FlashlightIndicatorColor

data class FlashlightIndicatorActions(
    val onToggleEnabled: (Boolean) -> Unit,
    val onToggleTapToDismiss: (Boolean) -> Unit,
    val onSetColor: (FlashlightIndicatorColor) -> Unit,
    val onToggleFlashlight: () -> Unit,
    val onEnableServiceWithShizuku: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onBack: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightIndicatorScreen(
    uiState: FlashlightIndicatorUiState,
    actions: FlashlightIndicatorActions
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashlight Indicator") },
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
            // Live Status Bar Mock Preview Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Status Bar Preview", style = MaterialTheme.typography.titleMedium)

                    // Mock Status Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Left clock
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("12:00", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }

                        // Dead Center Horizontal Flashlight Indicator
                        if (uiState.isIndicatorEnabled) {
                            val tintColor = Color(uiState.indicatorColor.colorArgb)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color(0xDC181818), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0x50FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_statusbar_flashlight_horizontal),
                                    contentDescription = "Flashlight Active",
                                    tint = tintColor,
                                    modifier = Modifier.size(width = 30.dp, height = 15.dp)
                                )
                            }
                        }

                        // Right battery
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("100%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }

            // Master Feature Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isIndicatorEnabled) {
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
                            text = "Status Bar Flashlight Indicator",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Displays a clean horizontal icon in the dead center of the status bar whenever the flashlight is on.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = uiState.isIndicatorEnabled,
                        onCheckedChange = actions.onToggleEnabled
                    )
                }
            }

            // Accessibility Service Connection Status
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Accessibility Service", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Bar Overlay Engine:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isServiceConnected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Active", color = Color(0xFF2E7D32))
                            } else {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disabled", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (!uiState.isServiceConnected) {
                        Text(
                            text = "The accessibility overlay is required to draw the indicator on top of the status bar in Android 12+.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isShizukuReady) {
                                Button(
                                    onClick = actions.onEnableServiceWithShizuku,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Enable with Shizuku")
                                }
                            }
                            OutlinedButton(
                                onClick = actions.onOpenAccessibilitySettings,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("System Settings")
                            }
                        }
                    }
                }
            }

            // Appearance & Behavior Preferences Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Appearance & Behavior", style = MaterialTheme.typography.titleMedium)

                    Text("Indicator Color", style = MaterialTheme.typography.bodyMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlashlightIndicatorColor.entries.forEach { colorOption ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.indicatorColor == colorOption,
                                    onClick = { actions.onSetColor(colorOption) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(colorOption.colorArgb), RoundedCornerShape(7.dp))
                                        .border(1.dp, Color.Gray, RoundedCornerShape(7.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(colorOption.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tap Indicator to Turn Off")
                            Text(
                                text = "When off, all status bar touches pass through smoothly. When on, tapping the indicator directly turns off the flashlight.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = uiState.isTapToDismiss,
                            onCheckedChange = actions.onToggleTapToDismiss
                        )
                    }
                }
            }

            // Interactive Testing Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hardware Testing", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Flashlight State:")
                        Text(
                            text = if (uiState.isFlashlightOn) "ON" else "OFF",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (uiState.isFlashlightOn) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = actions.onToggleFlashlight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isFlashlightOn) "Turn Flashlight OFF" else "Turn Flashlight ON")
                    }
                }
            }

            // Explanation Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Accidental Activation Protection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "On the Titan 2 Elite, the programmable physical side key is frequently mapped to toggle the flashlight. Because it is an external hardware button, it can easily turn on in pockets or while holding the device. This dead-center status bar icon ensures you are immediately aware when the flashlight is active.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
