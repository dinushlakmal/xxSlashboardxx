package org.slashboard.ime.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.slashboard.ime.engine.InputMode
import org.slashboard.ime.ime.KeyboardGeometry
import org.slashboard.ime.ime.KeyboardLayer
import org.slashboard.ime.ime.KeyboardLayoutFactory
import org.slashboard.ime.ime.LayoutPreloadCache
import org.slashboard.ime.settings.KeyboardPreferences

class SlashboardSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = KeyboardPreferences(applicationContext)
        val density = applicationContext.resources.displayMetrics.density
        val screenWidth = applicationContext.resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = applicationContext.resources.displayMetrics.heightPixels.toFloat()
        
        // We calculate both portrait and landscape widths if we can, but typically the shorter dimension is portrait width
        val portraitWidth = minOf(screenWidth, screenHeight)
        val landscapeWidth = maxOf(screenWidth, screenHeight)
        
        // Trim dictionary/learning data to prevent memory bloat
        val learningStore = LocalLearningStore(applicationContext)
        // A dummy access to load words and trim internally if needed.
        learningStore.words()
        
        // Warmup dictionaries in background
        val predictionRepo = PredictionRepository(applicationContext, learningStore)
        predictionRepo.warmup()
        
        // Pre-calculate standard layouts for fast retrieval when keyboard is opened
        try {
            preloadLayouts(portraitWidth, density, prefs, false)
            preloadLayouts(landscapeWidth, density, prefs, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    private fun preloadLayouts(width: Float, density: Float, prefs: KeyboardPreferences, isLandscape: Boolean) {
        val modes = listOf(InputMode.SMART_PHONETIC, InputMode.WIJESEKARA)
        val layers = listOf(KeyboardLayer.LETTERS, KeyboardLayer.NUMBERS, KeyboardLayer.SYMBOLS)
        
        for (mode in modes) {
            for (layer in layers) {
                // Simulate what KeyboardView.bindTyping does
                val rows = KeyboardLayoutFactory.typingRows(
                    mode = mode,
                    layer = layer,
                    shifted = false,
                    caps = false,
                    editor = org.slashboard.ime.ime.EditorLayout.TEXT,
                    topRow = prefs.topRow,
                    emojiPicker = prefs.emojiPicker,
                    enterLabel = "↵",
                    spaceLabel = " ",
                    offerGlobe = false,
                    isEnglish = prefs.useEnglish
                )
                val rowHeight = KeyboardGeometry.rowHeightPx(prefs.keyboardSize, isLandscape, density, rows.size)
                
                val layout = KeyboardLayoutFactory.place(
                    rows = rows,
                    width = width,
                    rowHeight = rowHeight,
                    insetH = KeyboardGeometry.visualInsetH(density, prefs.keySpacing),
                    insetV = KeyboardGeometry.visualInsetV(density, prefs.keySpacing),
                    sliver = KeyboardGeometry.SLIVER_DP * density
                )
                
                LayoutPreloadCache.put(
                    width = width,
                    rowHeight = rowHeight,
                    density = density,
                    mode = mode.name,
                    layer = layer.name,
                    topRow = prefs.topRow,
                    isEnglish = prefs.useEnglish,
                    keySpacing = prefs.keySpacing,
                    layout = layout
                )
            }
        }
    }
}
