package com.t2e.toolbox.eqbirvin.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.t2e.toolbox.eqbirvin.gesture.DetectedShadeState

private const val TAG = "ShadeStateDetector"

/**
 * Inspects accessibility windows and node hierarchy in com.android.systemui to determine
 * whether the shade is CLOSED, open to NOTIFICATIONS, or expanded to QUICK_SETTINGS.
 */
object ShadeStateDetector {

    private val QS_VIEW_IDS = listOf(
        "com.android.systemui:id/quick_settings_panel",
        "com.android.systemui:id/qs_pager",
        "com.android.systemui:id/brightness_slider",
        "com.android.systemui:id/qs_footer_actions",
        "com.android.systemui:id/qs_footer",
        "com.android.systemui:id/tile_page"
    )

    private val NOTIF_VIEW_IDS = listOf(
        "com.android.systemui:id/notification_stack_scroller",
        "com.android.systemui:id/notification_panel",
        "com.android.systemui:id/shared_notification_container"
    )

    fun detect(service: AccessibilityService): DetectedShadeState {
        val windowList = try {
            service.windows
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException retrieving windows", e)
            emptyList()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException retrieving windows", e)
            emptyList()
        }

        for (window in windowList) {
            val root = try {
                window.root
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException getting window root", e)
                null
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException getting window root", e)
                null
            } ?: continue

            val pkg = root.packageName?.toString() ?: ""
            if (pkg == "com.android.systemui") {
                for (qsId in QS_VIEW_IDS) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(qsId)
                    if (!nodes.isNullOrEmpty()) {
                        val isVisible = nodes.any { it.isVisibleToUser }
                        if (isVisible) {
                            return DetectedShadeState.QUICK_SETTINGS
                        }
                    }
                }

                for (notifId in NOTIF_VIEW_IDS) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(notifId)
                    if (!nodes.isNullOrEmpty()) {
                        val isVisible = nodes.any { it.isVisibleToUser }
                        if (isVisible) {
                            return DetectedShadeState.NOTIFICATIONS
                        }
                    }
                }
            }
        }

        val activeRoot = try {
            service.rootInActiveWindow
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting rootInActiveWindow", e)
            null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException getting rootInActiveWindow", e)
            null
        }
        if (activeRoot != null) {
            val pkg = activeRoot.packageName?.toString() ?: ""
            if (pkg == "com.android.systemui") {
                for (qsId in QS_VIEW_IDS) {
                    val qsNodes = activeRoot.findAccessibilityNodeInfosByViewId(qsId)
                    if (!qsNodes.isNullOrEmpty()) {
                        val isVisible = qsNodes.any { it.isVisibleToUser }
                        if (isVisible) {
                            return DetectedShadeState.QUICK_SETTINGS
                        }
                    }
                }
                for (notifId in NOTIF_VIEW_IDS) {
                    val notifNodes = activeRoot.findAccessibilityNodeInfosByViewId(notifId)
                    if (!notifNodes.isNullOrEmpty()) {
                        val isVisible = notifNodes.any { it.isVisibleToUser }
                        if (isVisible) {
                            return DetectedShadeState.NOTIFICATIONS
                        }
                    }
                }
            }
        }

        return DetectedShadeState.CLOSED
    }
}
