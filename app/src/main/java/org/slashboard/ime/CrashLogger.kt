package org.slashboard.ime

import android.content.Context
import java.io.File
import java.io.PrintWriter

object CrashLogger {
    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "crash.txt")
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                PrintWriter(file).use { pw ->
                    throwable.printStackTrace(pw)
                }
            } catch (e: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
