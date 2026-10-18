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
                                "In-memory input filter is connected and monitoring events.",
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
                                    "Enable T2E Toolbox in Accessibility Settings to intercept keyboard events and focus.",
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
                            "Suppress capacitive keyboard scrolling during typing to eliminate accidental page jumps.",
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

            // Blocking Methodologies
            AnimatedVisibility(visible = uiState.isEnabled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Suppression Methods", style = MaterialTheme.typography.titleMedium)

                        // Option 1: Keystroke Debounce
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "While Actively Typing (Debounce)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Suppresses trackpad scroll upon any physical keystroke, releasing after a pause.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = uiState.isBlockOnKeystroke,
                                onCheckedChange = actions.onToggleBlockOnKeystroke
                            )
                        }

                        HorizontalDivider()

                        // Option 2: Text Field Focus
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "While Text Field Focused (Continuous)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Suppresses trackpad scroll continuously whenever an editable text field is active.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = uiState.isBlockOnTextFieldFocus,
                                onCheckedChange = actions.onToggleBlockOnTextFieldFocus
                            )
                        }
                    }
                }
            }

            // Debounce Settings (shown when keystroke method is active)
            AnimatedVisibility(visible = uiState.isEnabled && uiState.isBlockOnKeystroke) {
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

            // Interactive Live Typing & Focus Test Area
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Interactive Focus & Typing Test", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Click the field below to test focus detection, then type with the physical keyboard to test keystroke detection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Live Status Badge
                    val statusColor by animateColorAsState(
                        targetValue = when {
                            !uiState.isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                            uiState.isScrollSuppressed -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        label = "badgeColor"
                    )

                    val statusTextColor = when {
                        !uiState.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        uiState.isScrollSuppressed -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    val statusText = when {
                        !uiState.isEnabled -> "Feature Disabled (Scroll Always Active)"
                        uiState.isScrollSuppressed -> when {
                            uiState.isTypingActive && uiState.isTextFieldFocused ->
                                "Scroll Blocked (Typing & Text Field Focused)"
                            uiState.isTextFieldFocused ->
                                "Scroll Blocked (Text Field Focused)"
                            uiState.isTypingActive ->
                                "Scroll Blocked (Keystroke Debounce Active)"
                            else -> "Scroll Blocked"
                        }
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

                    // Status Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(if (uiState.isTextFieldFocused) "Focus: Field Active" else "Focus: None")
                            }
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(if (uiState.isTypingActive) "Keystroke: Typing" else "Keystroke: Idle")
                            }
                        )
                    }

                    OutlinedTextField(
                        value = testTypingText,
                        onValueChange = { testTypingText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tap here to focus text field...") },
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
                            "Filtering operates dynamically in-memory via the Android Accessibility motion event stream without writing to flash storage. Normal touch events on the front glass screen remain completely unaffected at all times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
