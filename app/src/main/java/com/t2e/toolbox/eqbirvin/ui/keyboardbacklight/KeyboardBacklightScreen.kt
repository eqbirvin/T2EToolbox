package com.t2e.toolbox.eqbirvin.ui.keyboardbacklight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class KeyboardBacklightActions(
    val onToggleCustomEnabled: (Boolean) -> Unit,
    val onSetTimeoutSeconds: (Int) -> Unit,
    val onToggleKeepOnWithScreen: (Boolean) -> Unit,
    val onSetBrightness: (Int) -> Unit,
    val onApplyNow: () -> Unit,
    val onRefresh: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onClearStatusMessage: () -> Unit,
    val onBack: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardBacklightScreen(
    uiState: KeyboardBacklightUiState,
    actions: KeyboardBacklightActions
) {
    val focusManager = LocalFocusManager.current
    var textInputSeconds by remember(uiState.timeoutSeconds) {
        mutableStateOf(uiState.timeoutSeconds.toString())
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            actions.onClearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Backlight") },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shizuku Status Banner
            if (!uiState.isShizukuReady) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Shizuku Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            "Keyboard backlight adjustments require Shizuku to communicate with Unihertz's vendor service (agui_functional_service).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { actions.onRequestPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Grant Shizuku Permission")
                        }
                    }
                }
            }

            // Master Custom Switch Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Backlight Control", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Override stock timeout and maintain continuous illumination while screen is on.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isCustomEnabled,
                        onCheckedChange = { actions.onToggleCustomEnabled(it) },
                        enabled = uiState.isShizukuReady
                    )
                }
            }

            // Card 2: Keep On With Screen Option
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = uiState.isCustomEnabled && uiState.isShizukuReady) {
                                actions.onToggleKeepOnWithScreen(!uiState.keepOnWithScreen)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.keepOnWithScreen,
                            onCheckedChange = { actions.onToggleKeepOnWithScreen(it) },
                            enabled = uiState.isCustomEnabled && uiState.isShizukuReady
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Keep on with screen", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Backlight remains continuously lit while the screen is on and shuts off immediately when the screen turns off.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Card 3: Configurable Field in Seconds (Up to 5 minutes)
            val isTimeoutAdjustable = uiState.isCustomEnabled && !uiState.keepOnWithScreen && uiState.isShizukuReady

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = if (!uiState.keepOnWithScreen) CardDefaults.cardColors() else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Backlight Timeout", style = MaterialTheme.typography.titleMedium)
                        if (uiState.keepOnWithScreen) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Controlled by Screen", style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        } else {
                            val mins = uiState.timeoutSeconds / 60
                            val secs = uiState.timeoutSeconds % 60
                            val timeStr = if (mins > 0) "${mins}m ${secs.toString().padStart(2, '0')}s" else "${secs}s"
                            Text(
                                "${uiState.timeoutSeconds}s ($timeStr)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (uiState.keepOnWithScreen) {
                        Text(
                            "Timeout is currently set to continuous while the screen is active. Uncheck 'Keep on with screen' above to set a specific timeout in seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "How long the backlight stays lit after a key press (5 seconds up to 5 minutes / 300 seconds):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Direct Numeric Input Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = textInputSeconds,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(3)
                                    textInputSeconds = filtered
                                    filtered.toIntOrNull()?.let { num ->
                                        if (num in 5..300) {
                                            actions.onSetTimeoutSeconds(num)
                                        }
                                    }
                                },
                                label = { Text("Timeout (Seconds)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        textInputSeconds.toIntOrNull()?.let { num ->
                                            val clamped = num.coerceIn(5, 300)
                                            textInputSeconds = clamped.toString()
                                            actions.onSetTimeoutSeconds(clamped)
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f),
                                enabled = isTimeoutAdjustable
                            )

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    textInputSeconds.toIntOrNull()?.let { num ->
                                        val clamped = num.coerceIn(5, 300)
                                        textInputSeconds = clamped.toString()
                                        actions.onSetTimeoutSeconds(clamped)
                                    }
                                },
                                enabled = isTimeoutAdjustable
                            ) {
                                Text("Set")
                            }
                        }

                        // Slider (5s to 300s)
                        Slider(
                            value = uiState.timeoutSeconds.toFloat(),
                            onValueChange = { floatVal ->
                                val intVal = floatVal.toInt()
                                textInputSeconds = intVal.toString()
                                actions.onSetTimeoutSeconds(intVal)
                            },
                            valueRange = 5f..300f,
                            steps = 58, // roughly steps of 5 seconds (295 / 5 = 59 intervals)
                            enabled = isTimeoutAdjustable,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Preset Chips
                        Text("Presets:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(
                                15 to "15s",
                                30 to "30s",
                                60 to "1 min",
                                120 to "2 min",
                                300 to "5 min"
                            )
                            presets.forEach { (sec, label) ->
                                val selected = uiState.timeoutSeconds == sec
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        textInputSeconds = sec.toString()
                                        actions.onSetTimeoutSeconds(sec)
                                    },
                                    label = { Text(label) },
                                    enabled = isTimeoutAdjustable
                                )
                            }
                        }
                    }
                }
            }

            // Card 4: Brightness Slider (0 to 100%)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Backlight Brightness", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${uiState.brightness}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        "Keyboard illumination intensity level (0% to 100%):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = uiState.brightness.toFloat(),
                        onValueChange = { actions.onSetBrightness(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = uiState.isCustomEnabled && uiState.isShizukuReady,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Brightness Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Off", 20 to "Low", 30 to "Default", 60 to "Medium", 100 to "Max").forEach { (lvl, label) ->
                            FilterChip(
                                selected = uiState.brightness == lvl,
                                onClick = { actions.onSetBrightness(lvl) },
                                label = { Text(label) },
                                enabled = uiState.isCustomEnabled && uiState.isShizukuReady
                            )
                        }
                    }
                }
            }

            // Card 5: Live Hardware Status
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hardware Status", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { actions.onRefresh() }) {
                            Text("Refresh")
                        }
                    }
                    val hwTimeout = uiState.liveDeviceTimeoutSeconds?.let {
                        if (it >= 86400) "Continuous (24h+)" else "${it}s (${it / 60}m ${it % 60}s)"
                    } ?: "Unavailable (Check Shizuku)"
                    val hwBrightness = uiState.liveDeviceBrightness?.let { "$it%" } ?: "Unavailable"

                    Text("Active Device Timeout: $hwTimeout", style = MaterialTheme.typography.bodySmall)
                    Text("Active Device Brightness: $hwBrightness", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Apply Now Button
            Button(
                onClick = { actions.onApplyNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = uiState.isShizukuReady && !uiState.isApplying
            ) {
                if (uiState.isApplying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Applying...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Backlight Configuration")
                }
            }
        }
    }
}
