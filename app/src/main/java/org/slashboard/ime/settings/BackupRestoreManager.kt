package org.slashboard.ime.settings

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreManager(private val context: Context) {

    private val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
    private val themesDir = context.getDir("themes", Context.MODE_PRIVATE)

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zos ->
                    // Backup SharedPreferences
                    val prefsToBackup = listOf(
                        "slashboard_keyboard_preferences.xml",
                        "slashboard_learning.xml",
                        "slashboard_clipboard.xml"
                    )
                    
                    if (sharedPrefsDir.exists()) {
                        prefsToBackup.forEach { prefName ->
                            val prefFile = File(sharedPrefsDir, prefName)
                            if (prefFile.exists()) {
                                addFileToZip(zos, prefFile, "shared_prefs/$prefName")
                            }
                        }
                    }

                    // Backup Themes
                    if (themesDir.exists()) {
                        val themeFiles = themesDir.listFiles()
                        themeFiles?.forEach { themeFile ->
                            addFileToZip(zos, themeFile, "themes/${themeFile.name}")
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        
                        if (name.startsWith("shared_prefs/")) {
                            val fileName = name.substringAfter("shared_prefs/")
                            val targetFile = File(sharedPrefsDir, fileName)
                            if (!sharedPrefsDir.exists()) sharedPrefsDir.mkdirs()
                            extractFile(zis, targetFile)
                        } else if (name.startsWith("themes/")) {
                            val fileName = name.substringAfter("themes/")
                            val targetFile = File(themesDir, fileName)
                            if (!themesDir.exists()) themesDir.mkdirs()
                            extractFile(zis, targetFile)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, zipPath: String) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(zipPath)
            zos.putNextEntry(entry)
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    private fun extractFile(zis: ZipInputStream, targetFile: File) {
        FileOutputStream(targetFile).use { fos ->
            zis.copyTo(fos)
        }
    }
}
