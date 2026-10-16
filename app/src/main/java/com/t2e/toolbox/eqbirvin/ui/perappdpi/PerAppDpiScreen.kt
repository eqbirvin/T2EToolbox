package com.t2e.toolbox.eqbirvin.ui.perappdpi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t2e.toolbox.eqbirvin.data.DpiScaleCalculator
import com.t2e.toolbox.eqbirvin.data.DpiScalingMode
import com.t2e.toolbox.eqbirvin.data.PerAppDpiRule
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus

data class PerAppDpiActions(
    val onToggleEnabled: (Boolean) -> Unit,
    val onSetBaseDpi: (Int?) -> Unit,
    val onSaveRule: (PerAppDpiRule) -> Unit,
    val onDeleteRule: (String) -> Unit,
    val onOpenAppPicker: () -> Unit,
    val onCloseAppPicker: () -> Unit,
    val onOpenRuleEditor: (PerAppDpiRule) -> Unit,
    val onCloseRuleEditor: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onRefresh: () -> Unit,
    val onBack: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppDpiScreen(
    uiState: PerAppDpiUiState,
    actions: PerAppDpiActions
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Per-App Scaling (DPI)") },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = actions.onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shizuku Status Warning
            if (!uiState.isShizukuReady) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (uiState.shizukuStatus) {
                                        ShizukuStatus.NO_PERMISSION -> "Shizuku Permission Required"
                                        ShizukuStatus.NOT_RUNNING -> "Shizuku is Not Running"
                                        ShizukuStatus.NOT_INSTALLED -> "Shizuku Not Installed"
                                        else -> "Shizuku Unavailable"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dynamic display density scaling requires active Shizuku access to run window manager commands.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (uiState.shizukuStatus == ShizukuStatus.NO_PERMISSION) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = actions.onRequestPermission,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Grant Shizuku Permission")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Master Toggle Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Per-App Scaling",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scales selected apps to tablet density to eliminate vertical squishing and cutoff on Titan 2 Elite's square screen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = actions.onToggleEnabled,
                                enabled = uiState.isShizukuReady
                            )
                        }
                    }
                }
            }

            // Screen Diagnostics & Intelligent Range Legend Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Display Diagnostics & Intelligent Legend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val currentSw = DpiScaleCalculator.calculateSmallestWidthForDpi(
                            uiState.shortestSidePx,
                            uiState.activeDpi
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Active Density", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${uiState.activeDpi} DPI",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.activeDpi != (uiState.userConfiguredBaseDpi ?: uiState.activeBaseDpi)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Column {
                                Text("Base Density", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${uiState.userConfiguredBaseDpi ?: uiState.activeBaseDpi} DPI",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                Text("Shortest Side", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${uiState.shortestSidePx} px",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                Text("Smallest Width", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "$currentSw dp",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Layout Range Key (Dynamic for ${uiState.shortestSidePx}px screen):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Phone Layout:", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${uiState.rangeLegend.phoneDpRange} (${uiState.rangeLegend.phoneDpiRange})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Tablet Layout (Recommended):",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${uiState.rangeLegend.tabletDpRange} (${uiState.rangeLegend.tabletDpiRange})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Desktop Layout:", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${uiState.rangeLegend.desktopDpRange} (${uiState.rangeLegend.desktopDpiRange})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section Header & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured Apps (${uiState.rules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = actions.onOpenAppPicker,
                        enabled = uiState.isEnabled && uiState.isShizukuReady
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add App")
                    }
                }
            }

            // Rules List or Empty State
            if (uiState.rules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No apps configured yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+ Add App' to force Instagram, Spotify, or any other app to render in tablet mode. T2E Toolbox automatically restores your normal density upon leaving.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.rules, key = { it.packageName }) { rule ->
                    val appInfo = uiState.installedApps.firstOrNull { it.packageName == rule.packageName }
                    val targetDpi = DpiScaleCalculator.resolveTargetDpi(rule, uiState.shortestSidePx)
                    val swDp = DpiScaleCalculator.calculateSmallestWidthForDpi(uiState.shortestSidePx, targetDpi)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(drawable = appInfo?.icon, sizeDp = 44)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rule.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { actions.onOpenRuleEditor(rule) },
                                        label = {
                                            Text(
                                                text = when (rule.mode) {
                                                    DpiScalingMode.TABLET_600 -> "Tablet (600 dp)"
                                                    DpiScalingMode.DESKTOP_720 -> "Desktop (720 dp)"
                                                    DpiScalingMode.CUSTOM_DP -> "${rule.customValue ?: 600} dp"
                                                    DpiScalingMode.CUSTOM_DPI -> "${rule.customValue ?: 300} DPI"
                                                },
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                    Text(
                                        text = "$targetDpi DPI ($swDp dp)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            IconButton(onClick = { actions.onOpenRuleEditor(rule) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit rule")
                            }
                            IconButton(onClick = { actions.onDeleteRule(rule.packageName) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // App Picker Dialog
    if (uiState.isAppPickerOpen) {
        val configuredPackageNames = uiState.rules.map { it.packageName }.toSet()
        AppPickerDialog(
            installedApps = uiState.installedApps,
            alreadyConfiguredPackages = configuredPackageNames,
            isLoading = uiState.isLoadingApps,
            onAppSelected = { app ->
                actions.onCloseAppPicker()
                val defaultRule = PerAppDpiRule(
                    packageName = app.packageName,
                    appName = app.label,
                    mode = DpiScalingMode.TABLET_600
                )
                actions.onOpenRuleEditor(defaultRule)
            },
            onDismiss = actions.onCloseAppPicker
        )
    }

    // Rule Edit Dialog
    uiState.editingRule?.let { rule ->
        RuleEditDialog(
            rule = rule,
            shortestSidePx = uiState.shortestSidePx,
            legend = uiState.rangeLegend,
            onSave = actions.onSaveRule,
            onDismiss = actions.onCloseRuleEditor
        )
    }
}
