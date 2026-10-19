package com.t2e.toolbox.eqbirvin.data

import kotlinx.serialization.Serializable

@Serializable
enum class DpiScalingMode(val label: String, val defaultDp: Int? = null) {
    TABLET_600("Tablet (600 dp)", 600),
    DESKTOP_720("Desktop (720 dp)", 720),
    CUSTOM_DP("Custom Width (dp)", null),
    CUSTOM_DPI("Custom Density (DPI)", null)
}

@Serializable
data class PerAppDpiRule(
    val packageName: String,
    val appName: String,
    val mode: DpiScalingMode,
    val customValue: Int? = null
)

data class LayoutRangeLegend(
    val shortestSidePx: Int,
    val tabletBreakpointDpi: Int,
    val desktopBreakpointDpi: Int,
    val phoneDpiRange: String,
    val tabletDpiRange: String,
    val desktopDpiRange: String,
    val phoneDpRange: String = "< 600 dp",
    val tabletDpRange: String = "600 – 719 dp",
    val desktopDpRange: String = "≥ 720 dp"
)

/**
 * Decides whether a window-state change comes from a transient overlay that floats on top of the
 * real foreground app (volume panel, notification shade, permission prompts, Circle to Search,
 * Assistant / Gemini overlay, chat bubbles, ...). Such windows must never change the density,
 * otherwise the scaled app underneath is relaunched (reset -> relaunch -> re-apply loop).
 */
object TransientOverlayClassifier {
    const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    /** Packages whose windows are always shown on top of another app. */
    val OVERLAY_PACKAGES: Set<String> = setOf(
        SYSTEM_UI_PACKAGE,
        // Runtime permission prompts ("Allow Instagram to take pictures?")
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        // Install / uninstall confirmation dialogs
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    /** Class-name markers for Google app overlays (Circle to Search / Lens, Assistant, Gemini). */
    private val GOOGLE_OVERLAY_CLASS_MARKERS = listOf("lens", "assist", "gemini", ".opa.")

    /**
     * @param isApplicationWindow `true` if the event's window is a regular app window
     *   (`AccessibilityWindowInfo.TYPE_APPLICATION`), `false` if it is a system / voice-interaction /
     *   overlay window, or `null` if the window metadata isn't available yet.
     */
    fun isTransientOverlay(packageName: String, className: String, isApplicationWindow: Boolean?): Boolean {
        if (packageName in OVERLAY_PACKAGES) return true
        // Non-application windows (voice interaction sessions used by Circle to Search and the
        // Assistant / Gemini overlay, TYPE_APPLICATION_OVERLAY bubbles, system dialogs) never
        // represent a new foreground app.
        if (isApplicationWindow == false) return true
        // Fallback when window metadata is unavailable: Google app overlays identified by class.
        if (packageName == GOOGLE_APP_PACKAGE) {
            val lower = className.lowercase()
            if (GOOGLE_OVERLAY_CLASS_MARKERS.any { lower.contains(it) }) return true
        }
        return false
    }
}

object DpiScaleCalculator {
    const val TABLET_BREAKPOINT_DP = 600
    const val DESKTOP_BREAKPOINT_DP = 720

    /**
     * Calculates the required DPI to achieve a target smallest width in dp:
     * dp = (shortestSidePx * 160) / dpi => dpi = (shortestSidePx * 160) / dp
     */
    fun calculateDpiForSmallestWidth(shortestSidePx: Int, targetSwDp: Int): Int {
        if (targetSwDp <= 0 || shortestSidePx <= 0) return 300
        return ((shortestSidePx * 160.0) / targetSwDp).toInt().coerceIn(72, 600)
    }

    /**
     * Calculates the smallest width in dp for a given DPI and shortest side:
     * dp = (shortestSidePx * 160) / dpi
     */
    fun calculateSmallestWidthForDpi(shortestSidePx: Int, dpi: Int): Int {
        if (dpi <= 0 || shortestSidePx <= 0) return 0
        return ((shortestSidePx * 160.0) / dpi).toInt()
    }

    /**
     * Generates layout category ranges for the current screen's shortest dimension.
     * Phone: sw < 600dp (DPI > tabletBreakpointDpi)
     * Tablet: 600dp <= sw < 720dp (desktopBreakpointDpi < DPI <= tabletBreakpointDpi)
     * Desktop: sw >= 720dp (DPI <= desktopBreakpointDpi)
     */
    fun getRangeLegend(shortestSidePx: Int): LayoutRangeLegend {
        val tabletDpi = calculateDpiForSmallestWidth(shortestSidePx, TABLET_BREAKPOINT_DP)
        val desktopDpi = calculateDpiForSmallestWidth(shortestSidePx, DESKTOP_BREAKPOINT_DP)
        return LayoutRangeLegend(
            shortestSidePx = shortestSidePx,
            tabletBreakpointDpi = tabletDpi,
            desktopBreakpointDpi = desktopDpi,
            phoneDpiRange = "≥ ${tabletDpi + 1} DPI",
            tabletDpiRange = "${desktopDpi + 1} – $tabletDpi DPI",
            desktopDpiRange = "≤ $desktopDpi DPI"
        )
    }

    /**
     * Resolves the target DPI for a rule given the current shortest side dimension.
     */
    fun resolveTargetDpi(rule: PerAppDpiRule, shortestSidePx: Int): Int {
        return when (rule.mode) {
            DpiScalingMode.TABLET_600 -> calculateDpiForSmallestWidth(shortestSidePx, TABLET_BREAKPOINT_DP)
            DpiScalingMode.DESKTOP_720 -> calculateDpiForSmallestWidth(shortestSidePx, DESKTOP_BREAKPOINT_DP)
            DpiScalingMode.CUSTOM_DP -> {
                val targetDp = rule.customValue ?: TABLET_BREAKPOINT_DP
                calculateDpiForSmallestWidth(shortestSidePx, targetDp)
            }
            DpiScalingMode.CUSTOM_DPI -> {
                (rule.customValue ?: 300).coerceIn(72, 600)
            }
        }
    }
}
