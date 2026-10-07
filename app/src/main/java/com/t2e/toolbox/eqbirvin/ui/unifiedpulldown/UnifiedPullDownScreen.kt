package com.t2e.toolbox.eqbirvin.ui.unifiedpulldown

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.t2e.toolbox.eqbirvin.data.PullDownMethod
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus

data class UnifiedPullDownActions(
    val onToggleEnabled: (Boolean) -> Unit,
    val onToggleLockscreen: (Boolean) -> Unit,
    val onToggleDebugTint: (Boolean) -> Unit,
    val onSelectMethod: (PullDownMethod) -> Unit,
    val onEnableWithShizuku: () -> Unit,
    val onDisableWithShizuku: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onTestNotifications: () -> Unit,
    val onTestQuickSettings: () -> Unit,
    val onRefresh: () -> Unit,
    val onBack: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPullDownScreen(
    uiState: UnifiedPullDownUiState,
    actions: UnifiedPullDownActions
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                actions.onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unified Pull-Down") },
                navigationIcon = {
                    Button(onClick = actions.onBack) { Text("Back") }
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
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Stock-Style Status Bar Swipe", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Replaces the vendor's split touch behavior (left=notifications, right=Quick Settings) with stock Android behavior:\n" +
                        "1. Swiping down anywhere opens notifications.\n" +
                        "2. Swiping down again opens Quick Settings."
                    )
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Accessibility Service Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    when (uiState.serviceState) {
                        ServiceConnectionState.ENABLED_AND_CONNECTED -> {
                            Text("Enabled & Connected", color = MaterialTheme.colorScheme.primary)
                        }
                        ServiceConnectionState.ENABLED_NOT_CONNECTED -> {
                            Text("Enabled in system, connecting...", color = MaterialTheme.colorScheme.tertiary)
                        }
                        ServiceConnectionState.DISABLED -> {
                            Text("Disabled", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.serviceState == ServiceConnectionState.DISABLED) {
                            Button(
                                onClick = actions.onEnableWithShizuku,
                                enabled = !uiState.isWorking && uiState.shizukuStatus == ShizukuStatus.READY,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Enable via Shizuku")
                            }
                        } else {
                            OutlinedButton(
                                onClick = actions.onDisableWithShizuku,
                                enabled = !uiState.isWorking && uiState.shizukuStatus == ShizukuStatus.READY,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disable via Shizuku")
                            }
                        }

                        Button(
                            onClick = actions.onOpenAccessibilitySettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("System Settings")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: On Android 13+, if the accessibility toggle is greyed out, go to App info > ⋮ > \"Allow restricted settings\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    uiState.shizukuMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Unified Pull-Down",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = actions.onToggleEnabled,
                            enabled = uiState.serviceState == ServiceConnectionState.ENABLED_AND_CONNECTED
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Active on lock screen",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.isActiveOnLockscreen,
                            onCheckedChange = actions.onToggleLockscreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Show overlay area (debug tint)",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = uiState.isDebugTintEnabled,
                            onCheckedChange = actions.onToggleDebugTint
                        )
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trigger Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = uiState.method == PullDownMethod.ACCESSIBILITY,
                            onClick = { actions.onSelectMethod(PullDownMethod.ACCESSIBILITY) }
                        )
                        Text(
                            text = "Accessibility Service (performGlobalAction)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = uiState.method == PullDownMethod.SHIZUKU,
                            onClick = { actions.onSelectMethod(PullDownMethod.SHIZUKU) }
                        )
                        Text(
                            text = "Shizuku Shell (cmd statusbar expand-*)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Test Shade Expansion", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Test actions through the selected method to verify behavior on this ROM.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = actions.onTestNotifications,
                            enabled = uiState.canTest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Notifications")
                        }

                        Button(
                            onClick = actions.onTestQuickSettings,
                            enabled = uiState.canTest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Quick Settings")
                        }
                    }

                    uiState.testDisabledReason?.let { reason ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Test unavailable: $reason",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
