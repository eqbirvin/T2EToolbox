package com.t2e.toolbox.eqbirvin.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.t2e.toolbox.eqbirvin.T2EApplication
import com.t2e.toolbox.eqbirvin.data.LastRunRecord

private const val TAG = "DisplayFixBootWorker"

class DisplayFixBootWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? T2EApplication ?: run {
            Log.e(TAG, "Application context is not an instance of T2EApplication")
            return Result.failure()
        }

        val settingsRepo = app.settingsRepository
        val displayRepo = app.displayFixRepository

        if (settingsRepo.isDisplayFixEnabled.value) {
            val bound = displayRepo.waitForShizuku()
            if (bound) {
                val success = displayRepo.enableFix("Boot")
                showNotification(applicationContext, success, settingsRepo.lastRunRecord.value?.errorMsg)
            } else {
                val errorMsg = "Shizuku did not start"
                settingsRepo.saveLastRunRecord(
                    LastRunRecord(
                        System.currentTimeMillis(), "Boot", false, errorMsg
                    )
                )
                showNotification(applicationContext, false, errorMsg)
            }
        }

        return Result.success()
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

        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
        }
    }
}
