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
