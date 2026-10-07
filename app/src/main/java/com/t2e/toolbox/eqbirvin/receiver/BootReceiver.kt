package com.t2e.toolbox.eqbirvin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.t2e.toolbox.eqbirvin.worker.DisplayFixBootWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Rule 27: filter irrelevant events first
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<DisplayFixBootWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "display_fix_boot",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
