package org.slashboard.ime.ime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

interface VoiceInputListener {
    fun onReady() {}
    fun onBeginningOfSpeech() {}
    fun onRmsChanged(rmsdB: Float) {}
    fun onPartialResult(text: String) {}
    fun onVoiceResult(text: String) {}
    fun onError(error: Int) {}
    fun onStateChanged(isListening: Boolean, status: String) {}
}

class VoiceInputManager(
    private val context: Context,
    private val onVoiceResult: (String) -> Unit = {},
    private val onPartialResult: (String) -> Unit = {},
    private val onError: (Int) -> Unit = {},
    private val onReady: () -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<VoiceInputListener>()
    
    var isListening: Boolean = false
        private set

    fun addListener(listener: VoiceInputListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: VoiceInputListener) {
        listeners.remove(listener)
    }

    fun toggleListening(useEnglish: Boolean) {
        if (isListening) {
            stopListening()
        } else {
            startListening(useEnglish)
        }
    }
    
    fun startListening(useEnglish: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(context, org.slashboard.ime.settings.PermissionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            notifyError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    mainHandler.post {
                        onReady()
                        listeners.forEach { it.onReady(); it.onStateChanged(true, "Listening...") }
                    }
                }

                override fun onBeginningOfSpeech() {
                    mainHandler.post {
                        listeners.forEach { it.onBeginningOfSpeech() }
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    mainHandler.post {
                        listeners.forEach { it.onRmsChanged(rmsdB) }
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    mainHandler.post {
                        listeners.forEach { it.onStateChanged(false, "Processing...") }
                    }
                }

                override fun onError(error: Int) {
                    isListening = false
                    mainHandler.post {
                        onError(error)
                        listeners.forEach { it.onError(error); it.onStateChanged(false, "Tap mic to speak") }
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    mainHandler.post {
                        if (text.isNotEmpty()) {
                            onVoiceResult(text)
                            listeners.forEach { it.onVoiceResult(text); it.onStateChanged(false, "Done") }
                        } else {
                            listeners.forEach { it.onStateChanged(false, "Tap mic to speak") }
                        }
                    }
                }

                override fun onPartialResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    mainHandler.post {
                        if (text.isNotEmpty()) {
                            onPartialResult(text)
                            listeners.forEach { it.onPartialResult(text) }
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                val lang = if (useEnglish) "en-US" else "si-LK"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, lang)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            notifyStateChanged(true, "Connecting...")
        } catch (e: Exception) {
            isListening = false
            notifyError(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    private fun notifyError(error: Int) {
        isListening = false
        mainHandler.post {
            onError(error)
            listeners.forEach { it.onError(error); it.onStateChanged(false, "Tap mic to speak") }
        }
    }

    private fun notifyStateChanged(listening: Boolean, status: String) {
        mainHandler.post {
            listeners.forEach { it.onStateChanged(listening, status) }
        }
    }

    fun stopListening() {
        try {
            isListening = false
            speechRecognizer?.stopListening()
            notifyStateChanged(false, "Tap mic to speak")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        try {
            isListening = false
            speechRecognizer?.destroy()
            speechRecognizer = null
            listeners.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

