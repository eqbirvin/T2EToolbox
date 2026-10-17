package com.t2e.toolbox.eqbirvin.ui.typingblock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingScrollBlockScreen(
    uiState: TypingScrollBlockUiState,
    actions: TypingScrollBlockActions
) {
    var testTypingText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Block Scroll While Typing") },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            // Service Status Banner
            if (uiState.isServiceConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "Accessibility Service Active",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Hardware key filtering is connected and ready.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column {
                                Text(
                                    "Accessibility Service Required",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Enable T2E Toolbox in Accessibility Settings to intercept keyboard keypresses.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Button(
                            onClick = actions.onOpenAccessibilitySettings,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Open Accessibility Settings")
                        }
                    }
                }
            }

            // Master Toggle
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Block Scroll While Typing",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Temporarily suppress capacitive keyboard scrolling during typing to eliminate accidental page jumps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = uiState.isEnabled,
                        onCheckedChange = actions.onToggleEnabled
                    )
                }
            }

            // Debounce Settings
            AnimatedVisibility(visible = uiState.isEnabled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Debounce Duration", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${uiState.debounceMs} ms",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            "Delay after the last keystroke before trackpad scrolling automatically resumes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Slider(
                            value = uiState.debounceMs.toFloat(),
                            onValueChange = { actions.onSetDebounceMs(it.toInt()) },
                            valueRange = 300f..2500f,
                            steps = 21
                        )

                        Text("Quick Presets:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                400 to "400ms (Fast)",
                                800 to "800ms (Default)",
                                1200 to "1200ms",
                                1600 to "1600ms"
                            ).forEach { (ms, label) ->
                                val isSelected = uiState.debounceMs == ms
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { actions.onSetDebounceMs(ms) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Live Typing Test Area
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Interactive Typing Test", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Type below using the physical keyboard. Watch the status indicator switch in real-time when typing begins and pauses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Live Status Badge
                    val statusColor by animateColorAsState(
                        targetValue = when {
                            !uiState.isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                            uiState.isTypingActive -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        label = "badgeColor"
                    )

                    val statusTextColor = when {
                        !uiState.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        uiState.isTypingActive -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    val statusText = when {
                        !uiState.isEnabled -> "Feature Disabled (Scroll Always Active)"
                        uiState.isTypingActive -> "Typing Detected — Trackpad Scroll Blocked!"
                        else -> "Idle — Trackpad Scroll Allowed"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusColor, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusTextColor
                        )
                    }

                    OutlinedTextField(
                        value = testTypingText,
                        onValueChange = { testTypingText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type here with physical keyboard...") },
                        maxLines = 3
                    )

                    if (testTypingText.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { testTypingText = "" }) {
                                Text("Clear Test Field")
                            }
                        }
                    }
                }
            }

            // How It Works Explainer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "How This Works",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "When physical keys are pressed, T2E Toolbox intercepts the capacitive touchpad stream at the Android InputDispatcher level, preventing accidental swipes from bouncing your active app. Normal touchscreen touches on the display glass remain completely unaffected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
