package com.t2e.toolbox.eqbirvin.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.t2e.toolbox.eqbirvin.data.DisplayFixRepository
import com.t2e.toolbox.eqbirvin.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsRepo = SettingsRepository(context)
                    if (settingsRepo.isDisplayFixEnabled.value) {
                        val displayRepo = DisplayFixRepository(settingsRepo)
                        val bound = displayRepo.waitForShizuku()
                        if (bound) {
                            val success = displayRepo.enableFix("Boot")
                            showNotification(context, success, settingsRepo.lastRunRecord.value?.errorMsg)
                        } else {
                            settingsRepo.saveLastRunRecord(
                                com.t2e.toolbox.eqbirvin.data.LastRunRecord(
                                    System.currentTimeMillis(), "Boot", false, "Shizuku did not start"
                                )
                            )
                            showNotification(context, false, "Shizuku did not start")
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, success: Boolean, errorMsg: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val channelId = "t2e_boot_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Boot Status", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (success) "Display Fix Applied" else "Display Fix Failed"
        val text = if (success) "Custom resolution applied successfully." else (errorMsg ?: "Unknown error")

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }
}
