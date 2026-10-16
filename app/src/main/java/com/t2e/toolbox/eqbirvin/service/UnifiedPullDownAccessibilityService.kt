package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
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
import com.t2e.toolbox.eqbirvin.gesture.DetectedShadeState
import com.t2e.toolbox.eqbirvin.gesture.PullDownAction
import com.t2e.toolbox.eqbirvin.gesture.PullDownStateMachine
import com.t2e.toolbox.eqbirvin.gesture.resolveSmartAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "UnifiedPullDownService"

class UnifiedPullDownAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val swipeDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val stateMachine = PullDownStateMachine()

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var pullDownRepo: UnifiedPullDownRepository
    private lateinit var displayFixRepo: DisplayFixRepository
    private lateinit var perAppDpiRepo: com.t2e.toolbox.eqbirvin.data.PerAppDpiRepository

    private var windowManager: WindowManager? = null
    private var overlayView: StatusbarOverlayView? = null
    private var flashlightOverlayMgr: FlashlightOverlayManager? = null
    private var currentLayoutParams: WindowManager.LayoutParams? = null
    private var currentHeight: Int = 0

    private var isServiceConnected = false
    private var connectionJob: Job? = null
    private var isViewAttached = false
    private var isFeatureEnabled = false
    private var activeOnLockscreen = true
    private var isDebugTintEnabled = false
    private var isSmartShadeDetectionEnabled = true
    private var isHapticFeedbackEnabled = true
    private var currentMethod = PullDownMethod.ACCESSIBILITY
    private var isPassingThroughTap = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restoreTouchableRunnable = Runnable {
        restoreTouchable()
    }

    private val keyguardReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    serviceScope.launch(swipeDispatcher) {
                        stateMachine.reset()
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    updateOverlayDimensions()
                    updateVisibility()
                    flashlightOverlayMgr?.onConfigurationChanged()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as T2EApplication
        settingsRepo = app.settingsRepository
        pullDownRepo = app.unifiedPullDownRepository
        displayFixRepo = app.displayFixRepository
        perAppDpiRepo = app.perAppDpiRepository
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        flashlightOverlayMgr = FlashlightOverlayManager(this, wm, app.flashlightRepository, settingsRepo)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(keyguardReceiver, filter)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        pullDownRepo.setServiceConnected(true)

        connectionJob?.cancel()
        val job = SupervisorJob()
        connectionJob = job
        val connectionScope = CoroutineScope(serviceScope.coroutineContext + job)

        connectionScope.launch {
            settingsRepo.isPullDownEnabled.collect { enabled ->
                isFeatureEnabled = enabled
                syncOverlayAttachment()
                updateVisibility()
            }
        }

        connectionScope.launch {
            settingsRepo.isPullDownOnLockscreen.collect { onLock ->
                activeOnLockscreen = onLock
                updateVisibility()
            }
        }

        connectionScope.launch {
            settingsRepo.isPullDownDebugTint.collect { tint ->
                isDebugTintEnabled = tint
                updateDebugTint()
            }
        }

        connectionScope.launch {
            settingsRepo.pullDownMethod.collect { method ->
                currentMethod = method
            }
        }

        connectionScope.launch {
            settingsRepo.isSmartShadeDetectionEnabled.collect { enabled ->
                isSmartShadeDetectionEnabled = enabled
            }
        }

        connectionScope.launch {
            settingsRepo.isPullDownHapticFeedbackEnabled.collect { enabled ->
                isHapticFeedbackEnabled = enabled
                overlayView?.swipeHapticFeedbackEnabled = enabled
            }
        }

        connectionScope.launch {
            pullDownRepo.testActions.collect { action ->
                serviceScope.launch(swipeDispatcher) {
                    executeAction(action)
                }
            }
        }

        flashlightOverlayMgr?.start(connectionScope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: ""
            if (pkg.isNotEmpty() && pkg != "com.android.systemui") {
                serviceScope.launch(swipeDispatcher) {
                    stateMachine.reset()
                }
            }

            val className = event.className?.toString() ?: ""
            handlePerAppDpiWindowState(pkg, className)
        }
    }

    private fun handlePerAppDpiWindowState(pkg: String, className: String) {
        if (pkg.isEmpty()) return
        if (className.startsWith("android.widget.") || className.startsWith("android.view.")) {
            return
        }
        val isHomeOrRecents = perAppDpiRepo.isHomeOrRecentsPackage(pkg)
        perAppDpiRepo.onPackageForeground(pkg, isHomeOrRecents)
    }

    override fun onInterrupt() {
        // No interrupt action needed
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOverlayDimensions()
        flashlightOverlayMgr?.onConfigurationChanged()
        perAppDpiRepo.notifyConfigurationChanged(newConfig.densityDpi)
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
        if (isServiceConnected && isFeatureEnabled) {
            attachOverlay()
        } else {
            detachOverlay()
        }
    }

    private fun attachOverlay() {
        if (!isServiceConnected || isViewAttached) return
        val wm = windowManager ?: return
        val view = StatusbarOverlayView(
            context = this,
            swipeHapticFeedbackEnabled = isHapticFeedbackEnabled,
            onSwipeDetected = { onSwipe() },
            onTapDetected = { x, y -> passThroughTap(x, y) }
        ).also { overlayView = it }
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
        mainHandler.removeCallbacks(restoreTouchableRunnable)
        isPassingThroughTap = false
        try {
            windowManager?.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException removing overlay view", e)
        }
        overlayView = null
        isViewAttached = false
    }

    private fun checkKeyguardLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    private fun updateVisibility() {
        val view = overlayView ?: return
        val hideOnLock = !activeOnLockscreen && checkKeyguardLocked()
        val shouldBeVisible = isFeatureEnabled && !hideOnLock
        view.visibility = if (shouldBeVisible) View.VISIBLE else View.GONE
    }

    private fun passThroughTap(x: Float, y: Float) {
        if (isPassingThroughTap) return
        val view = overlayView ?: return
        val params = currentLayoutParams ?: return
        val wm = windowManager ?: return

        isPassingThroughTap = true
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try {
            wm.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException setting FLAG_NOT_TOUCHABLE", e)
            isPassingThroughTap = false
            return
        }

        mainHandler.postDelayed(restoreTouchableRunnable, 250)

        try {
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y + 1f)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 40)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    mainHandler.removeCallbacks(restoreTouchableRunnable)
                    restoreTouchable()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    mainHandler.removeCallbacks(restoreTouchableRunnable)
                    restoreTouchable()
                }
            }, null)

            if (!dispatched) {
                Log.w(TAG, "dispatchGesture returned false")
                mainHandler.removeCallbacks(restoreTouchableRunnable)
                restoreTouchable()
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException in passThroughTap gesture", e)
            mainHandler.removeCallbacks(restoreTouchableRunnable)
            restoreTouchable()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in passThroughTap gesture", e)
            mainHandler.removeCallbacks(restoreTouchableRunnable)
            restoreTouchable()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException in passThroughTap gesture", e)
            mainHandler.removeCallbacks(restoreTouchableRunnable)
            restoreTouchable()
        }
    }

    private fun restoreTouchable() {
        if (!isPassingThroughTap) return
        val view = overlayView ?: return
        val params = currentLayoutParams ?: return
        val wm = windowManager ?: return

        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try {
            wm.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException restoring touchable flag", e)
        } finally {
            isPassingThroughTap = false
        }
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
        serviceScope.launch(swipeDispatcher) {
            val action = if (isSmartShadeDetectionEnabled) {
                val shadeState = ShadeStateDetector.detect(this@UnifiedPullDownAccessibilityService)
                Log.d(TAG, "Smart shade detection detected state: $shadeState")
                resolveSmartAction(shadeState)
            } else {
                stateMachine.onSwipe()
            }
            executeAction(action)
        }
    }

    private suspend fun executeAction(action: PullDownAction) {
        if (action == PullDownAction.NONE) return
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

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceConnected = false
        connectionJob?.cancel()
        connectionJob = null
        pullDownRepo.setServiceConnected(false)
        detachOverlay()
        flashlightOverlayMgr?.stop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        connectionJob?.cancel()
        connectionJob = null
        pullDownRepo.setServiceConnected(false)
        mainHandler.removeCallbacks(restoreTouchableRunnable)
        try {
            unregisterReceiver(keyguardReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException unregistering keyguard receiver", e)
        }
        detachOverlay()
        overlayView = null
        flashlightOverlayMgr?.stop()
        flashlightOverlayMgr = null
        serviceScope.cancel()
    }
}
