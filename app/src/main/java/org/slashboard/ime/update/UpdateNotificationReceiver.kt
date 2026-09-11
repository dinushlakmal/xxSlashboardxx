package org.slashboard.ime.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "org.slashboard.ime.ACTION_UPDATE_NOW") {
            val url = intent.getStringExtra("downloadUrl")
            if (url != null) {
                val updateManager = UpdateManager(context)
                updateManager.startDownloadAndInstall(url)
            }
        }
    }
}
