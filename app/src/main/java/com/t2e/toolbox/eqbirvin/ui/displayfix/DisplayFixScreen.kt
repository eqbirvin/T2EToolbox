package com.t2e.toolbox.eqbirvin.ui.displayfix

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayFixScreen(
    uiState: DisplayFixUiState,
    onToggle: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display Fix") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target Device: Unihertz Titan 2 Elite", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Forces rendering to 864x960 at 231 dpi. Restores original values when turned off.")
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shizuku Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    when (uiState.shizukuStatus) {
                        ShizukuStatus.READY -> {
                            Text("Ready", color = MaterialTheme.colorScheme.primary)
                        }
                        ShizukuStatus.NOT_INSTALLED -> {
                            Text("Shizuku is not installed.", color = MaterialTheme.colorScheme.error)
                        }
                        ShizukuStatus.NOT_RUNNING -> {
                            Text("Shizuku is installed but not running.", color = MaterialTheme.colorScheme.error)
                        }
                        ShizukuStatus.NO_PERMISSION -> {
                            Text("Shizuku is running, but permission not granted.", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRequestPermission) {
                                Text("Request Permission")
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Custom display (864x960 @ 231 dpi)",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = uiState.isEnabled,
                    onCheckedChange = { enabled ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && enabled) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onToggle(enabled)
                    },
                    enabled = !uiState.isWorking && (uiState.isShizukuReady || !uiState.isEnabled)
                )
            }

            Button(
                onClick = onRunNow,
                enabled = uiState.isEnabled && uiState.isShizukuReady && !uiState.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Now")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.currentDisplayState?.let {
                        Text("Reported Size: ${it.size}")
                        Text("Reported Density: ${it.density}")
                    } ?: run {
                        Text("Reported State: Unknown")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.lastRunRecord?.let { record ->
                        val dateString = remember(record.timestamp) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
                        }
                        Text("Last run: $dateString (${record.trigger})")
                        if (record.success) {
                            Text("Result: Success", color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Result: Failed - ${record.errorMsg.orEmpty()}", color = MaterialTheme.colorScheme.error)
                        }
                    } ?: run {
                        Text("Last run: Never")
                    }
                }
            }
        }
    }
}
