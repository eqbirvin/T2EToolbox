package com.t2e.toolbox.eqbirvin.ui.perappdpi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.DpiScaleCalculator
import com.t2e.toolbox.eqbirvin.data.LayoutRangeLegend
import com.t2e.toolbox.eqbirvin.data.PerAppDpiRepository
import com.t2e.toolbox.eqbirvin.data.PerAppDpiRule
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfoItem(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
)

data class PerAppDpiUiState(
    val isEnabled: Boolean = false,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val activeDpi: Int = 300,
    val activeBaseDpi: Int = 300,
    val userConfiguredBaseDpi: Int? = null,
    val shortestSidePx: Int = 1080,
    val rangeLegend: LayoutRangeLegend = DpiScaleCalculator.getRangeLegend(1080),
    val rules: List<PerAppDpiRule> = emptyList(),
    val currentForegroundPackage: String? = null,
    val installedApps: List<AppInfoItem> = emptyList(),
    val isLoadingApps: Boolean = false,
    val isAppPickerOpen: Boolean = false,
    val editingRule: PerAppDpiRule? = null
) {
    val isShizukuReady: Boolean get() = shizukuStatus == ShizukuStatus.READY
}

class PerAppDpiViewModel(
    private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val perAppDpiRepo: PerAppDpiRepository,
    private val displayRepo: DisplayFixRepository
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(false)
    private val _isAppPickerOpen = MutableStateFlow(false)
    private val _editingRule = MutableStateFlow<PerAppDpiRule?>(null)

    val uiState: StateFlow<PerAppDpiUiState> = combine(
        settingsRepo.isPerAppDpiEnabled,
        displayRepo.shizukuStatus,
        perAppDpiRepo.activeDpi,
        perAppDpiRepo.activeBaseDpi,
        settingsRepo.perAppDpiBaseDpi
    ) { isEnabled, shizukuStatus, activeDpi, activeBaseDpi, userBaseDpi ->
        val shortestSide = perAppDpiRepo.getShortestSidePx()
        val legend = DpiScaleCalculator.getRangeLegend(shortestSide)
        PerAppDpiUiState(
            isEnabled = isEnabled,
            shizukuStatus = shizukuStatus,
            activeDpi = activeDpi,
            activeBaseDpi = activeBaseDpi,
            userConfiguredBaseDpi = userBaseDpi,
            shortestSidePx = shortestSide,
            rangeLegend = legend,
            currentForegroundPackage = perAppDpiRepo.currentForegroundPackage.value
        )
    }.combine(settingsRepo.perAppDpiRules) { state, rulesMap ->
        state.copy(rules = rulesMap.values.sortedBy { it.appName.lowercase() })
    }.combine(_installedApps) { state, apps ->
        state.copy(installedApps = apps)
    }.combine(_isLoadingApps) { state, loading ->
        state.copy(isLoadingApps = loading)
    }.combine(_isAppPickerOpen) { state, pickerOpen ->
        state.copy(isAppPickerOpen = pickerOpen)
    }.combine(_editingRule) { state, editing ->
        state.copy(editingRule = editing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerAppDpiUiState())

    init {
        refreshState()
        loadInstalledApps()
    }

    fun refreshState() {
        viewModelScope.launch {
            displayRepo.refreshShizukuStatus()
            perAppDpiRepo.refreshMetrics()
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setPerAppDpiEnabled(enabled)
            if (!enabled) {
                perAppDpiRepo.restoreBaseDensity(immediate = true)
            }
        }
    }

    fun setUserBaseDpi(dpi: Int?) {
        viewModelScope.launch {
            settingsRepo.setPerAppDpiBaseDpi(dpi)
        }
    }

    fun saveRule(rule: PerAppDpiRule) {
        viewModelScope.launch {
            perAppDpiRepo.addOrUpdateRule(rule)
            _editingRule.value = null
        }
    }

    fun deleteRule(packageName: String) {
        viewModelScope.launch {
            perAppDpiRepo.removeRule(packageName)
        }
    }

    fun openAppPicker() {
        _isAppPickerOpen.value = true
        if (_installedApps.value.isEmpty()) {
            loadInstalledApps()
        }
    }

    fun closeAppPicker() {
        _isAppPickerOpen.value = false
    }

    fun openRuleEditor(rule: PerAppDpiRule) {
        _editingRule.value = rule
    }

    fun closeRuleEditor() {
        _editingRule.value = null
    }

    fun onRequestPermission() {
        viewModelScope.launch {
            displayRepo.requestShizukuPermission()
        }
    }

    fun loadInstalledApps() {
        if (_isLoadingApps.value) return
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                resolveInfos.mapNotNull { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    val label = ri.loadLabel(pm).toString()
                    val icon = try {
                        ri.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }
                    AppInfoItem(packageName = pkg, label = label, icon = icon)
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }
}
