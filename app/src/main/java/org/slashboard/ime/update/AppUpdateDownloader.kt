package org.slashboard.ime.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateDownloader(private val context: Context) {

    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            var connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            // Handle redirects if any (GitHub assets usually redirect to object storage)
            if (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                connection.responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                val newUrl = connection.getHeaderField("Location")
                connection = URL(newUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP ${connection.responseCode}")
            }

            val fileLength = connection.contentLength
            val fileName = "Slashboard-update-${System.currentTimeMillis()}.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            
            if (destinationFile.exists()) destinationFile.delete()

            val input = connection.inputStream
            val output = FileOutputStream(destinationFile)
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            var lastProgress = 0
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            withContext(Dispatchers.Main) {
                onComplete(destinationFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    fun promptInstall(file: File) {
        try {
            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
