package com.t2e.toolbox.eqbirvin.data

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PerAppDpiRepository"
private const val SETTLE_DEBOUNCE_MS = 180L

class PerAppDpiRepository(
    private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _activeDpi = MutableStateFlow(readSystemDensity())
    val activeDpi: StateFlow<Int> = _activeDpi.asStateFlow()

    private val _activeBaseDpi = MutableStateFlow(readSystemDensity())
    val activeBaseDpi: StateFlow<Int> = _activeBaseDpi.asStateFlow()

    private val _currentForegroundPackage = MutableStateFlow<String?>(null)
    val currentForegroundPackage: StateFlow<String?> = _currentForegroundPackage.asStateFlow()

    @Volatile
    private var lastAppliedDpi: Int? = null

    private var applyJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Safe restoration to base density on screen-off / lock to prevent lockscreen distortion
                    repoScope.launch {
                        restoreBaseDensity(immediate = true)
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    // Refresh metrics on unlock
                    repoScope.launch {
                        refreshMetrics()
                    }
                }
            }
        }
    }

    init {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            context.registerReceiver(screenReceiver, filter)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to register screenReceiver", e)
        }

        repoScope.launch {
            refreshMetrics()
        }

        repoScope.launch {
            settingsRepo.isPerAppDpiEnabled.collect { enabled ->
                if (!enabled && lastAppliedDpi != null) {
                    restoreBaseDensity(immediate = true)
                }
            }
        }
    }

    fun getShortestSidePx(): Int {
        return try {
            val bounds = windowManager?.currentWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                minOf(bounds.width(), bounds.height())
            } else {
                1080
            }
        } catch (e: Exception) {
            1080
        }
    }

    private fun readSystemDensity(): Int {
        return context.resources.configuration.densityDpi.coerceIn(72, 600)
    }

    suspend fun refreshMetrics() = withContext(Dispatchers.IO) {
        val currentDpi = readSystemDensity()
        _activeDpi.value = currentDpi
        if (lastAppliedDpi == null) {
            _activeBaseDpi.value = currentDpi
        }
    }

    fun notifyConfigurationChanged(densityDpi: Int) {
        _activeDpi.value = densityDpi
        if (lastAppliedDpi == null) {
            _activeBaseDpi.value = densityDpi
        }
    }

    private var cachedHomePackages: Set<String> = emptySet()
    private var lastHomeQueryTime: Long = 0L

    fun isHomeOrRecentsPackage(packageName: String): Boolean {
        if (packageName == "com.android.systemui") return true
        val now = System.currentTimeMillis()
        if (now - lastHomeQueryTime > 30000L || cachedHomePackages.isEmpty()) {
            val pm = context.packageManager
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val homeActivities = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                cachedHomePackages = homeActivities.map { it.activityInfo.packageName }.toSet()
                lastHomeQueryTime = now
            } catch (e: Exception) {
                // Keep existing cache
            }
        }
        return cachedHomePackages.contains(packageName)
    }

    fun onPackageForeground(packageName: String?, isHomeOrRecents: Boolean) {
        if (!settingsRepo.isPerAppDpiEnabled.value) return
        if (packageName.isNullOrEmpty() || packageName == "android") {
            return
        }

        // Immediate reversion to base on entering Home, Recents, or SystemUI
        if (isHomeOrRecents || isHomeOrRecentsPackage(packageName)) {
            _currentForegroundPackage.value = packageName
            applyJob?.cancel()
            repoScope.launch {
                restoreBaseDensity(immediate = true)
            }
            return
        }

        if (packageName == context.packageName) {
            _currentForegroundPackage.value = packageName
            applyJob?.cancel()
            repoScope.launch {
                restoreBaseDensity(immediate = false)
            }
            return
        }

        _currentForegroundPackage.value = packageName

        val rules = settingsRepo.perAppDpiRules.value
        val rule = rules[packageName]

        applyJob?.cancel()
        applyJob = repoScope.launch {
            delay(SETTLE_DEBOUNCE_MS)

            // Screen safety guard
            val isInteractive = powerManager?.isInteractive ?: true
            val isLocked = keyguardManager?.isKeyguardLocked ?: false
            if (!isInteractive || isLocked) {
                return@launch
            }

            if (!ShizukuShell.isReady()) {
                Log.w(TAG, "Shizuku is not ready; cannot apply per-app density for $packageName")
                return@launch
            }

            val shortestSide = getShortestSidePx()

            if (rule != null) {
                val targetDpi = DpiScaleCalculator.resolveTargetDpi(rule, shortestSide)
                if (targetDpi != lastAppliedDpi) {
                    val cmd = "wm density $targetDpi"
                    val result = ShizukuShell.runShell(cmd)
                    if (result.exitCode == 0) {
                        lastAppliedDpi = targetDpi
                        _activeDpi.value = targetDpi
                        Log.i(TAG, "Applied per-app DPI $targetDpi (${rule.mode}) for $packageName")
                    } else {
                        Log.e(TAG, "Failed to apply density: ${result.stderr}")
                    }
                }
            } else {
                // Untargeted app: revert back to base
                restoreBaseDensity(immediate = false)
            }
        }
    }

    suspend fun restoreBaseDensity(immediate: Boolean = false) = withContext(Dispatchers.IO) {
        if (lastAppliedDpi == null) return@withContext
        if (!ShizukuShell.isReady()) {
            lastAppliedDpi = null
            return@withContext
        }

        val userConfiguredBase = settingsRepo.perAppDpiBaseDpi.value
        val baseDpi = userConfiguredBase ?: _activeBaseDpi.value

        val cmd = if (userConfiguredBase == null && baseDpi == 300) {
            "wm density reset"
        } else {
            "wm density $baseDpi"
        }

        val result = ShizukuShell.runShell(cmd)
        if (result.exitCode == 0) {
            lastAppliedDpi = null
            _activeDpi.value = baseDpi
            Log.i(TAG, "Restored base density $baseDpi (immediate=$immediate, cmd: $cmd)")
        } else {
            Log.e(TAG, "Failed to restore base density: ${result.stderr}")
        }
    }

    suspend fun addOrUpdateRule(rule: PerAppDpiRule) = withContext(Dispatchers.IO) {
        val current = settingsRepo.perAppDpiRules.value.toMutableMap()
        current[rule.packageName] = rule
        settingsRepo.savePerAppDpiRules(current)

        // If the updated app is currently in the foreground, re-evaluate immediately
        if (_currentForegroundPackage.value == rule.packageName) {
            onPackageForeground(rule.packageName, isHomeOrRecents = false)
        }
    }

    suspend fun removeRule(packageName: String) = withContext(Dispatchers.IO) {
        val current = settingsRepo.perAppDpiRules.value.toMutableMap()
        current.remove(packageName)
        settingsRepo.savePerAppDpiRules(current)

        if (_currentForegroundPackage.value == packageName) {
            restoreBaseDensity(immediate = true)
        }
    }
}
