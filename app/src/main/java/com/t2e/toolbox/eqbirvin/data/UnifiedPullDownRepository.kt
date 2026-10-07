package com.t2e.toolbox.eqbirvin.data

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.t2e.toolbox.eqbirvin.gesture.PullDownAction
import com.t2e.toolbox.eqbirvin.service.UnifiedPullDownAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "UnifiedPullDownRepo"

data class ShizukuOperationResult(
    val success: Boolean,
    val message: String
)

class UnifiedPullDownRepository(
    private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _testActions = MutableSharedFlow<PullDownAction>(extraBufferCapacity = 1)
    val testActions: SharedFlow<PullDownAction> = _testActions.asSharedFlow()

    fun setServiceConnected(connected: Boolean) {
        _serviceConnected.value = connected
    }

    suspend fun sendTestCommand(action: PullDownAction) {
        _testActions.emit(action)
    }

    suspend fun isServiceEnabled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val targetComponent = ComponentName(context, UnifiedPullDownAccessibilityService::class.java)
            val fullFlattened = targetComponent.flattenToString()
            val shortFlattened = targetComponent.flattenToShortString()

            val servicesList = enabledServices.split(":").map { it.trim() }
            servicesList.contains(fullFlattened) || servicesList.contains(shortFlattened)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading enabled_accessibility_services", e)
            false
        }
    }

    suspend fun enableWithShizuku(): ShizukuOperationResult = withContext(Dispatchers.IO) {
        try {
            val currentServicesResult = ShizukuShell.runShell("settings get secure enabled_accessibility_services")
            if (currentServicesResult.exitCode != 0) {
                return@withContext ShizukuOperationResult(false, "Failed to read enabled services: ${currentServicesResult.stderr}")
            }
            val currentAccessEnabledResult = ShizukuShell.runShell("settings get secure accessibility_enabled")
            if (currentAccessEnabledResult.exitCode != 0) {
                return@withContext ShizukuOperationResult(false, "Failed to read accessibility_enabled: ${currentAccessEnabledResult.stderr}")
            }

            val rawCurrentServices = currentServicesResult.stdout.trim().let { if (it == "null") "" else it }
            val rawCurrentEnabled = currentAccessEnabledResult.stdout.trim()

            // Snapshot prior state if not already snapshotted
            if (settingsRepo.accessibilitySnapshot.value == null) {
                settingsRepo.saveAccessibilitySnapshot(
                    AccessibilitySettingsSnapshot(
                        previousEnabledServices = rawCurrentServices,
                        previousAccessibilityEnabled = rawCurrentEnabled
                    )
                )
            }

            val targetComponent = ComponentName(context, UnifiedPullDownAccessibilityService::class.java).flattenToString()
            val existingList = if (rawCurrentServices.isEmpty()) {
                emptyList()
            } else {
                rawCurrentServices.split(":").map { it.trim() }.filter { it.isNotEmpty() }
            }

            val newList = if (existingList.contains(targetComponent)) {
                existingList
            } else {
                existingList + targetComponent
            }

            val newServicesString = newList.joinToString(":")
            val setServicesResult = ShizukuShell.runShell("settings put secure enabled_accessibility_services ${ShizukuShell.shellQuote(newServicesString)}")
            if (setServicesResult.exitCode != 0) {
                return@withContext ShizukuOperationResult(false, "Failed to update enabled services: ${setServicesResult.stderr}")
            }

            val setEnabledResult = ShizukuShell.runShell("settings put secure accessibility_enabled 1")
            if (setEnabledResult.exitCode != 0) {
                return@withContext ShizukuOperationResult(false, "Failed to set accessibility_enabled: ${setEnabledResult.stderr}")
            }

            ShizukuOperationResult(true, "Accessibility service enabled via Shizuku.")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during enableWithShizuku", e)
            ShizukuOperationResult(false, "Error: ${e.message.orEmpty()}")
        }
    }

    suspend fun disableWithShizuku(): ShizukuOperationResult = withContext(Dispatchers.IO) {
        try {
            val currentServicesResult = ShizukuShell.runShell("settings get secure enabled_accessibility_services")
            if (currentServicesResult.exitCode != 0) {
                return@withContext ShizukuOperationResult(false, "Failed to read enabled services: ${currentServicesResult.stderr}")
            }
            val rawCurrentServices = currentServicesResult.stdout.trim().let { if (it == "null") "" else it }
            val targetComponent = ComponentName(context, UnifiedPullDownAccessibilityService::class.java).flattenToString()
            val targetShort = ComponentName(context, UnifiedPullDownAccessibilityService::class.java).flattenToShortString()

            val existingList = if (rawCurrentServices.isEmpty()) {
                emptyList()
            } else {
                rawCurrentServices.split(":").map { it.trim() }.filter { it.isNotEmpty() }
            }

            val newList = existingList.filter { it != targetComponent && it != targetShort }
            val newServicesString = newList.joinToString(":")

            val setServicesResult = if (newServicesString.isEmpty()) {
                ShizukuShell.runShell("settings delete secure enabled_accessibility_services")
            } else {
                ShizukuShell.runShell("settings put secure enabled_accessibility_services ${ShizukuShell.shellQuote(newServicesString)}")
            }

            if (setServicesResult.exitCode != 0) {
                return@withContext ShizukuOperationResult(false, "Failed to update enabled services: ${setServicesResult.stderr}")
            }

            val snapshot = settingsRepo.accessibilitySnapshot.value
            if (newList.isEmpty()) {
                val restoreAccessResult = if (snapshot == null) {
                    ShizukuShell.runShell("settings put secure accessibility_enabled 0")
                } else if (snapshot.previousAccessibilityEnabled == null ||
                    snapshot.previousAccessibilityEnabled == "null" ||
                    snapshot.previousAccessibilityEnabled.isEmpty()
                ) {
                    ShizukuShell.runShell("settings delete secure accessibility_enabled")
                } else {
                    ShizukuShell.runShell("settings put secure accessibility_enabled ${ShizukuShell.shellQuote(snapshot.previousAccessibilityEnabled)}")
                }
                if (restoreAccessResult.exitCode != 0) {
                    return@withContext ShizukuOperationResult(false, "Failed to restore accessibility_enabled: ${restoreAccessResult.stderr}")
                }
            }

            settingsRepo.saveAccessibilitySnapshot(null)
            ShizukuOperationResult(true, "Accessibility service disabled via Shizuku.")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during disableWithShizuku", e)
            ShizukuOperationResult(false, "Error: ${e.message.orEmpty()}")
        }
    }
}
