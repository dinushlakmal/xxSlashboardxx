package org.slashboard.ime.ime

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe pre-computed cache for keyboard layouts across orientations and modes.
 * Layouts placed here in the background by WorkManager or Coroutines can be retrieved
 * instantaneously on the main UI thread during keyboard display and configuration switches.
 */
internal object LayoutPreloadCache {
    private data class CacheKey(
        val width: Int,
        val rowHeight: Int,
        val densityInt: Int,
        val mode: String,
        val layer: String,
        val topRow: String,
        val isEnglish: Boolean,
        val keySpacing: String,
        val shifted: Boolean = false,
        val capsLock: Boolean = false
    )

    private val cache = ConcurrentHashMap<CacheKey, KeyboardLayout>()

    fun get(
        width: Float,
        rowHeight: Float,
        density: Float,
        mode: String,
        layer: String,
        topRow: String,
        isEnglish: Boolean,
        keySpacing: String,
        shifted: Boolean = false,
        capsLock: Boolean = false
    ): KeyboardLayout? {
        val key = CacheKey(
            width = width.toInt(),
            rowHeight = rowHeight.toInt(),
            densityInt = (density * 100).toInt(),
            mode = mode,
            layer = layer,
            topRow = topRow,
            isEnglish = isEnglish,
            keySpacing = keySpacing,
            shifted = shifted,
            capsLock = capsLock
        )
        return cache[key]
    }

    fun put(
        width: Float,
        rowHeight: Float,
        density: Float,
        mode: String,
        layer: String,
        topRow: String,
        isEnglish: Boolean,
        keySpacing: String,
        shifted: Boolean = false,
        capsLock: Boolean = false,
        layout: KeyboardLayout
    ) {
        val key = CacheKey(
            width = width.toInt(),
            rowHeight = rowHeight.toInt(),
            densityInt = (density * 100).toInt(),
            mode = mode,
            layer = layer,
            topRow = topRow,
            isEnglish = isEnglish,
            keySpacing = keySpacing,
            shifted = shifted,
            capsLock = capsLock
        )
        cache[key] = layout
    }

    fun clear() {
        cache.clear()
    }
}
