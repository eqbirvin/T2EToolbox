package com.t2e.toolbox.eqbirvin.ui.perappdpi

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.t2e.toolbox.eqbirvin.data.DpiScaleCalculator
import com.t2e.toolbox.eqbirvin.data.DpiScalingMode
import com.t2e.toolbox.eqbirvin.data.LayoutRangeLegend
import com.t2e.toolbox.eqbirvin.data.PerAppDpiRule

@Composable
fun AppIcon(
    drawable: Drawable?,
    modifier: Modifier = Modifier,
    sizeDp: Int = 40
) {
    val bitmap = remember(drawable) {
        drawable?.let {
            try {
                it.toBitmap(width = 64, height = 64).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(sizeDp.dp)
        )
    } else {
        Surface(
            modifier = modifier.size(sizeDp.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size((sizeDp / 2).dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppPickerDialog(
    installedApps: List<AppInfoItem>,
    alreadyConfiguredPackages: Set<String>,
    isLoading: Boolean,
    onAppSelected: (AppInfoItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, searchQuery, alreadyConfiguredPackages) {
        val query = searchQuery.trim().lowercase()
        installedApps.filter { app ->
            !alreadyConfiguredPackages.contains(app.packageName) &&
                    (query.isEmpty() ||
                            app.label.lowercase().contains(query) ||
                            app.packageName.lowercase().contains(query))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select App to Scale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No eligible apps found" else "No matching apps found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(app) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(drawable = app.icon, sizeDp = 40)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun RuleEditDialog(
    rule: PerAppDpiRule,
    shortestSidePx: Int,
    legend: LayoutRangeLegend,
    onSave: (PerAppDpiRule) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(rule.mode) }
    var customInput by remember {
        mutableStateOf(
            rule.customValue?.toString() ?: when (rule.mode) {
                DpiScalingMode.CUSTOM_DP -> "600"
                DpiScalingMode.CUSTOM_DPI -> legend.tabletBreakpointDpi.toString()
                else -> ""
            }
        )
    }

    val parsedValue = customInput.toIntOrNull()
    val previewDpi = remember(selectedMode, parsedValue, shortestSidePx) {
        val tempRule = PerAppDpiRule(
            packageName = rule.packageName,
            appName = rule.appName,
            mode = selectedMode,
            customValue = parsedValue
        )
        DpiScaleCalculator.resolveTargetDpi(tempRule, shortestSidePx)
    }
    val previewSwDp = remember(previewDpi, shortestSidePx) {
        DpiScaleCalculator.calculateSmallestWidthForDpi(shortestSidePx, previewDpi)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Configure Scaling",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rule.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scaling Target",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode radio options
                DpiScalingMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    selectedMode = mode
                                    if (mode == DpiScalingMode.CUSTOM_DP && customInput.isEmpty()) {
                                        customInput = "600"
                                    } else if (mode == DpiScalingMode.CUSTOM_DPI && customInput.isEmpty()) {
                                        customInput = legend.tabletBreakpointDpi.toString()
                                    }
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = mode.label, style = MaterialTheme.typography.bodyMedium)
                            if (mode == DpiScalingMode.TABLET_600) {
                                Text(
                                    text = "Best for Instagram & vertical cutoff fixes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom input field if applicable
                if (selectedMode == DpiScalingMode.CUSTOM_DP || selectedMode == DpiScalingMode.CUSTOM_DPI) {
                    val labelText = if (selectedMode == DpiScalingMode.CUSTOM_DP) "Target Width (dp)" else "Target Density (DPI)"
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it.filter { char -> char.isDigit() } },
                        label = { Text(labelText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Range Key / Legend card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Intelligent Layout Ranges",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Phone: ${legend.phoneDpRange} (${legend.phoneDpiRange})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Tablet: ${legend.tabletDpRange} (${legend.tabletDpiRange})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Desktop: ${legend.desktopDpRange} (${legend.desktopDpiRange})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Active calculation: $previewDpi DPI yields $previewSwDp dp smallest width",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalRule = PerAppDpiRule(
                                packageName = rule.packageName,
                                appName = rule.appName,
                                mode = selectedMode,
                                customValue = if (selectedMode == DpiScalingMode.CUSTOM_DP || selectedMode == DpiScalingMode.CUSTOM_DPI) parsedValue else null
                            )
                            onSave(finalRule)
                        },
                        enabled = (selectedMode != DpiScalingMode.CUSTOM_DP && selectedMode != DpiScalingMode.CUSTOM_DPI) || parsedValue != null
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
