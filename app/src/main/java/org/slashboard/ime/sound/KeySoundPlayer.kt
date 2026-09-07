package org.slashboard.ime.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.util.Log
import org.slashboard.ime.R
import org.slashboard.ime.settings.KeyboardPreferences
import java.util.concurrent.ConcurrentHashMap

class KeySoundPlayer private constructor(private val context: Context) {
    private val appContext = context.applicationContext
    private var soundPool: SoundPool? = null
    private var soundIosId = 0
    private var soundMechId = 0
    private var soundTypeId = 0
    private val loadedMap = ConcurrentHashMap<Int, Boolean>()
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    init {
        initSoundPool()
    }

    @Synchronized
    private fun initSoundPool() {
        if (soundPool != null) return
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attrs)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedMap[sampleId] = true
                }
            }

            soundIosId = pool.load(appContext, R.raw.sound_ios, 1)
            soundMechId = pool.load(appContext, R.raw.sound_mechanical, 1)
            soundTypeId = pool.load(appContext, R.raw.sound_typewriter, 1)

            soundPool = pool
        } catch (e: Exception) {
            Log.e("KeySoundPlayer", "Failed to initialize SoundPool", e)
        }
    }

    fun play(soundPack: String) {
        initSoundPool()
        val pool = soundPool
        var played = false

        if (pool != null) {
            try {
                val soundId = when (soundPack.lowercase()) {
                    "ios" -> soundIosId
                    "mechanical" -> soundMechId
                    "typewriter" -> soundTypeId
                    else -> 0
                }

                if (soundId != 0 && (loadedMap[soundId] == true || Build.VERSION.SDK_INT >= 30)) {
                    val streamId = pool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                    if (streamId != 0) {
                        played = true
                    }
                }
            } catch (e: Exception) {
                Log.w("KeySoundPlayer", "Error playing sound from sound pool: $soundPack", e)
            }
        }

        if (!played) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
            } catch (e: Exception) {
                // Ignore fallback error
            }
        }
    }

    fun playIfEnabled(prefs: KeyboardPreferences) {
        if (!prefs.keySounds) return
        play(prefs.soundPack)
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            loadedMap.clear()
        } catch (e: Exception) {
            Log.w("KeySoundPlayer", "Error releasing SoundPool", e)
        }
    }

    companion object {
        @Volatile
        private var instance: KeySoundPlayer? = null

        fun getInstance(context: Context): KeySoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: KeySoundPlayer(context).also { instance = it }
            }
        }
    }
}
