package org.slashboard.ime.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.slashboard.ime.BuildConfig
import org.slashboard.ime.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val manager = UpdateManager(context)
        val info = manager.checkForUpdates(BuildConfig.VERSION_NAME)
        if (info.hasUpdate) {
            val prefs = context.getSharedPreferences("slashboard_update_prefs", Context.MODE_PRIVATE)
            var firstSeen = prefs.getLong("update_first_seen_${info.latestVersion}", 0L)
            val now = System.currentTimeMillis()
            
            if (firstSeen == 0L) {
                firstSeen = now
                prefs.edit().putLong("update_first_seen_${info.latestVersion}", firstSeen).apply()
            }
            
            val twelveHoursInMillis = 12L * 60L * 60L * 1000L
            if (now - firstSeen >= twelveHoursInMillis) {
                val downloader = AppUpdateDownloader(context)
                downloader.downloadApk(
                    downloadUrl = info.downloadUrl,
                    onProgress = {},
                    onComplete = { file -> downloader.promptInstall(file) },
                    onError = {}
                )
            } else {
                showNotification(info)
            }
        }
        return Result.success()
    }

    private fun showNotification(info: UpdateInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "slashboard_updates"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Slashboard updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, UpdateNotificationReceiver::class.java).apply {
            action = "org.slashboard.ime.ACTION_UPDATE_NOW"
            putExtra("downloadUrl", info.downloadUrl)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 100, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Slashboard Update Available")
            .setContentText("Version ${info.latestVersion} is available. Tap to update.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Update Now", pendingIntent)

        notificationManager.notify(2001, builder.build())
    }

    companion object {
        fun scheduleDaily8AMCheck(context: Context) {
            runCatching {
                val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(2, TimeUnit.HOURS)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "SlashboardDailyUpdateCheck",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            }.onFailure { it.printStackTrace() }
        }
    }
}
