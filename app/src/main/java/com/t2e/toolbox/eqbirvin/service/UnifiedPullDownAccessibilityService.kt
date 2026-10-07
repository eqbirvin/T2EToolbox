package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.t2e.toolbox.eqbirvin.T2EApplication
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.PullDownMethod
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import com.t2e.toolbox.eqbirvin.data.ShizukuShell
import com.t2e.toolbox.eqbirvin.data.UnifiedPullDownRepository
import com.t2e.toolbox.eqbirvin.gesture.PullDownAction
import com.t2e.toolbox.eqbirvin.gesture.PullDownStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "UnifiedPullDownService"

class UnifiedPullDownAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stateMachine = PullDownStateMachine(clock = { SystemClock.elapsedRealtime() })

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var pullDownRepo: UnifiedPullDownRepository
    private lateinit var displayFixRepo: DisplayFixRepository

    private var windowManager: WindowManager? = null
    private var overlayView: StatusbarOverlayView? = null
    private var currentLayoutParams: WindowManager.LayoutParams? = null
    private var currentHeight: Int = 0

    private var isViewAttached = false
    private var isFeatureEnabled = false
    private var activeOnLockscreen = true
    private var isDebugTintEnabled = false
    private var currentMethod = PullDownMethod.ACCESSIBILITY
    private var isStatusBarVisible = true

    private val keyguardReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            if (action != Intent.ACTION_SCREEN_OFF && action != Intent.ACTION_USER_PRESENT && action != Intent.ACTION_SCREEN_ON) {
                return
            }
            updateVisibility()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as T2EApplication
        settingsRepo = app.settingsRepository
        pullDownRepo = app.unifiedPullDownRepository
        displayFixRepo = app.displayFixRepository
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(keyguardReceiver, filter)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        pullDownRepo.setServiceConnected(true)

        serviceScope.launch {
            settingsRepo.isPullDownEnabled.collect { enabled ->
                isFeatureEnabled = enabled
                syncOverlayAttachment()
                updateVisibility()
            }
        }

        serviceScope.launch {
            settingsRepo.isPullDownOnLockscreen.collect { onLock ->
                activeOnLockscreen = onLock
                updateVisibility()
            }
        }

        serviceScope.launch {
            settingsRepo.isPullDownDebugTint.collect { tint ->
                isDebugTintEnabled = tint
                updateDebugTint()
            }
        }

        serviceScope.launch {
            settingsRepo.pullDownMethod.collect { method ->
                currentMethod = method
            }
        }

        serviceScope.launch {
            pullDownRepo.testActions.collect { action ->
                executeAction(action)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Only draws an overlay; screen content events are filtered and ignored
    }

    override fun onInterrupt() {
        // No interrupt action needed
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOverlayDimensions()
    }

    private fun getStatusBarHeight(): Int {
        val wm = windowManager ?: return 0
        val metrics = wm.currentWindowMetrics
        val insets = metrics.windowInsets.getInsets(WindowInsets.Type.statusBars())
        val topInset = insets.top
        if (topInset > 0) {
            return topInset
        }
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    private fun updateOverlayDimensions() {
        val view = overlayView ?: return
        if (!isViewAttached) return
        val newHeight = getStatusBarHeight()
        if (newHeight > 0 && newHeight != currentHeight) {
            currentHeight = newHeight
            currentLayoutParams?.height = newHeight
            try {
                windowManager?.updateViewLayout(view, currentLayoutParams)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "IllegalArgumentException updating overlay layout", e)
            }
        }
    }

    private fun syncOverlayAttachment() {
        if (isFeatureEnabled) {
            attachOverlay()
        } else {
            detachOverlay()
        }
    }

    private fun attachOverlay() {
        if (isViewAttached) return
        val wm = windowManager ?: return
        val view = overlayView ?: StatusbarOverlayView(this) { onSwipe() }.also { overlayView = it }
        val height = getStatusBarHeight()
        currentHeight = height
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            if (height > 0) height else 100,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }
        currentLayoutParams = params
        updateDebugTint()

        view.setOnApplyWindowInsetsListener { _, insets ->
            val statusBarsVisible = insets.isVisible(WindowInsets.Type.statusBars())
            isStatusBarVisible = statusBarsVisible
            updateVisibility()
            insets
        }

        try {
            wm.addView(view, params)
            isViewAttached = true
            updateVisibility()
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException adding overlay view", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException adding overlay view", e)
        }
    }

    private fun detachOverlay() {
        if (!isViewAttached) return
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException removing overlay view", e)
        }
        isViewAttached = false
    }

    private fun checkKeyguardLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    private fun updateVisibility() {
        val view = overlayView ?: return
        val hideOnLock = !activeOnLockscreen && checkKeyguardLocked()
        val shouldBeVisible = isFeatureEnabled && isStatusBarVisible && !hideOnLock
        view.visibility = if (shouldBeVisible) View.VISIBLE else View.GONE
    }

    private fun updateDebugTint() {
        val view = overlayView ?: return
        if (isDebugTintEnabled) {
            view.setBackgroundColor(Color.argb(100, 255, 0, 0))
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun onSwipe() {
        val action = stateMachine.onSwipe()
        executeAction(action)
    }

    private fun executeAction(action: PullDownAction) {
        if (action == PullDownAction.NONE) return
        serviceScope.launch(Dispatchers.IO) {
            when (currentMethod) {
                PullDownMethod.ACCESSIBILITY -> {
                    when (action) {
                        PullDownAction.OPEN_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                        PullDownAction.OPEN_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                        PullDownAction.NONE -> {}
                    }
                }
                PullDownMethod.SHIZUKU -> {
                    if (displayFixRepo.isShizukuReady()) {
                        val cmd = when (action) {
                            PullDownAction.OPEN_NOTIFICATIONS -> "cmd statusbar expand-notifications"
                            PullDownAction.OPEN_QUICK_SETTINGS -> "cmd statusbar expand-settings"
                            PullDownAction.NONE -> ""
                        }
                        if (cmd.isNotEmpty()) {
                            val result = ShizukuShell.runShell(cmd)
                            if (result.exitCode != 0) {
                                Log.e(TAG, "Shizuku statusbar command failed with exitCode ${result.exitCode}: ${result.stderr}")
                            }
                        }
                    } else {
                        Log.w(TAG, "Shizuku method selected but Shizuku is not ready; command ignored")
                    }
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        pullDownRepo.setServiceConnected(false)
        detachOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        pullDownRepo.setServiceConnected(false)
        try {
            unregisterReceiver(keyguardReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException unregistering keyguard receiver", e)
        }
        detachOverlay()
        overlayView = null
        serviceScope.cancel()
    }
}
