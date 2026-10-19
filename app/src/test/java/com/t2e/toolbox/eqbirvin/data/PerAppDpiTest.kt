package com.t2e.toolbox.eqbirvin.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerAppDpiTest {

    @Test
    fun testStock1080pxDpiCalculations() {
        val shortestSide = 1080
        val tabletDpi = DpiScaleCalculator.calculateDpiForSmallestWidth(shortestSide, 600)
        assertEquals(288, tabletDpi)

        val swForTablet = DpiScaleCalculator.calculateSmallestWidthForDpi(shortestSide, tabletDpi)
        assertEquals(600, swForTablet)

        val desktopDpi = DpiScaleCalculator.calculateDpiForSmallestWidth(shortestSide, 720)
        assertEquals(240, desktopDpi)

        val swForDesktop = DpiScaleCalculator.calculateSmallestWidthForDpi(shortestSide, desktopDpi)
        assertEquals(720, swForDesktop)

        val stockSw = DpiScaleCalculator.calculateSmallestWidthForDpi(shortestSide, 300)
        assertEquals(576, stockSw)
    }

    @Test
    fun testCalibrated1076pxDpiCalculations() {
        val shortestSide = 1076
        val tabletDpi = DpiScaleCalculator.calculateDpiForSmallestWidth(shortestSide, 600)
        assertEquals(286, tabletDpi)

        val desktopDpi = DpiScaleCalculator.calculateDpiForSmallestWidth(shortestSide, 720)
        assertEquals(239, desktopDpi)
    }

    @Test
    fun testDisplayFix864pxDpiCalculations() {
        val shortestSide = 864
        val tabletDpi = DpiScaleCalculator.calculateDpiForSmallestWidth(shortestSide, 600)
        assertEquals(230, tabletDpi)

        val swForTablet = DpiScaleCalculator.calculateSmallestWidthForDpi(shortestSide, tabletDpi)
        assertTrue(swForTablet >= 600)

        val desktopDpi = DpiScaleCalculator.calculateDpiForSmallestWidth(shortestSide, 720)
        assertEquals(192, desktopDpi)

        val swForDesktop = DpiScaleCalculator.calculateSmallestWidthForDpi(shortestSide, desktopDpi)
        assertEquals(720, swForDesktop)
    }

    @Test
    fun testRangeLegendStock() {
        val legend = DpiScaleCalculator.getRangeLegend(1080)
        assertEquals(1080, legend.shortestSidePx)
        assertEquals(288, legend.tabletBreakpointDpi)
        assertEquals(240, legend.desktopBreakpointDpi)
        assertEquals("≥ 289 DPI", legend.phoneDpiRange)
        assertEquals("241 – 288 DPI", legend.tabletDpiRange)
        assertEquals("≤ 240 DPI", legend.desktopDpiRange)
    }

    @Test
    fun testRangeLegendDisplayFix() {
        val legend = DpiScaleCalculator.getRangeLegend(864)
        assertEquals(864, legend.shortestSidePx)
        assertEquals(230, legend.tabletBreakpointDpi)
        assertEquals(192, legend.desktopBreakpointDpi)
        assertEquals("≥ 231 DPI", legend.phoneDpiRange)
        assertEquals("193 – 230 DPI", legend.tabletDpiRange)
        assertEquals("≤ 192 DPI", legend.desktopDpiRange)
    }

    @Test
    fun testResolveTargetDpi() {
        val shortestSide = 1080

        val tabletRule = PerAppDpiRule("com.instagram.android", "Instagram", DpiScalingMode.TABLET_600)
        assertEquals(288, DpiScaleCalculator.resolveTargetDpi(tabletRule, shortestSide))

        val desktopRule = PerAppDpiRule("com.slack", "Slack", DpiScalingMode.DESKTOP_720)
        assertEquals(240, DpiScaleCalculator.resolveTargetDpi(desktopRule, shortestSide))

        val customDpRule = PerAppDpiRule("com.spotify.music", "Spotify", DpiScalingMode.CUSTOM_DP, 640)
        // (1080 * 160) / 640 = 270 DPI
        assertEquals(270, DpiScaleCalculator.resolveTargetDpi(customDpRule, shortestSide))

        val customDpiRule = PerAppDpiRule("com.example.test", "Test", DpiScalingMode.CUSTOM_DPI, 260)
        assertEquals(260, DpiScaleCalculator.resolveTargetDpi(customDpiRule, shortestSide))
    }

    @Test
    fun testRuleSerialization() {
        val rule = PerAppDpiRule(
            packageName = "com.instagram.android",
            appName = "Instagram",
            mode = DpiScalingMode.TABLET_600
        )
        val json = Json { ignoreUnknownKeys = true }
        val serialized = json.encodeToString(rule)
        val deserialized = json.decodeFromString<PerAppDpiRule>(serialized)

        assertEquals(rule.packageName, deserialized.packageName)
        assertEquals(rule.appName, deserialized.appName)
        assertEquals(rule.mode, deserialized.mode)
        assertEquals(rule.customValue, deserialized.customValue)

        val map = mapOf(rule.packageName to rule)
        val mapSerialized = json.encodeToString(map)
        val mapDeserialized = json.decodeFromString<Map<String, PerAppDpiRule>>(mapSerialized)
        assertEquals(1, mapDeserialized.size)
        assertEquals(rule, mapDeserialized["com.instagram.android"])
    }

    @Test
    fun testSystemUiAndPromptPackagesAreOverlays() {
        val overlayPackages = listOf(
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller"
        )
        for (pkg in overlayPackages) {
            // Even when reported as an application window (e.g. GrantPermissionsActivity)
            assertTrue(pkg, TransientOverlayClassifier.isTransientOverlay(pkg, "SomeClass", true))
            assertTrue(pkg, TransientOverlayClassifier.isTransientOverlay(pkg, "SomeClass", null))
        }
    }

    @Test
    fun testNonApplicationWindowsAreOverlays() {
        // Voice interaction sessions (Circle to Search, Gemini overlay), bubbles, etc.
        assertTrue(
            TransientOverlayClassifier.isTransientOverlay(
                "com.google.android.googlequicksearchbox", "com.google.SomeSession", false
            )
        )
        assertTrue(
            TransientOverlayClassifier.isTransientOverlay("com.facebook.orca", "ChatHeadView", false)
        )
    }

    @Test
    fun testGoogleAppOverlayClassFallback() {
        val gsa = "com.google.android.googlequicksearchbox"
        assertTrue(TransientOverlayClassifier.isTransientOverlay(gsa, "com.google.android.apps.lens.LensActivity", null))
        assertTrue(TransientOverlayClassifier.isTransientOverlay(gsa, "com.google.android.apps.gsa.staticplugins.opa.OpaActivity", null))
        assertTrue(TransientOverlayClassifier.isTransientOverlay(gsa, "com.google.android.apps.gemini.OverlayActivity", null))
        // Regular Google app search screen is a real foreground app
        assertFalse(TransientOverlayClassifier.isTransientOverlay(gsa, "com.google.android.googlequicksearchbox.SearchActivity", true))
    }

    @Test
    fun testRegularAppsAreNotOverlays() {
        assertFalse(TransientOverlayClassifier.isTransientOverlay("com.instagram.android", "com.instagram.mainactivity.MainActivity", true))
        assertFalse(TransientOverlayClassifier.isTransientOverlay("com.instagram.android", "com.instagram.mainactivity.MainActivity", null))
        // Class markers only apply to the Google app
        assertFalse(TransientOverlayClassifier.isTransientOverlay("com.example.lensapp", "com.example.lensapp.LensActivity", true))
    }
}
