package org.slashboard.ime.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slashboard.ime.data.*
import org.slashboard.ime.engine.*
import org.slashboard.ime.settings.KeyboardPreferences
import java.util.concurrent.Executors
import java.util.concurrent.Future
import android.media.SoundPool
import android.media.AudioAttributes

class SlashboardInputMethodService : InputMethodService(), KeyboardActions {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefs: KeyboardPreferences
    private lateinit var keyboard: KeyboardView
    private var soundPool: SoundPool? = null
    private var soundIos = 0
    private var soundMech = 0
    private var soundType = 0
    private var learning: LocalLearningStore? = null
    private var prediction: PredictionRepository? = null
    private var emoji: EmojiRepository? = null
    private var clipboardHistory: ClipboardHistoryStore? = null
    private val composition = CompositionSession()
    private val slsSource = StringBuilder()
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var predictionTask: Future<*>? = null
    private var generation = 0
    private var restricted = false
    private var lastSelectionEnd = -1
    private var previousCommittedWord: String? = null
    private var recentEmoji = mutableListOf<String>()
    private var editorLayout = EditorLayout.TEXT
    private var voiceInputManager: VoiceInputManager? = null

    private var precedingDirty = true
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this)
        restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }

    override fun onFinishInput() {
        deleteAnchor = -1
        deleteLength = 0
        cancelComposition(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        voiceInputManager?.destroy()
        soundPool?.release()
        stopClipboardListener()
        serviceScope.cancel()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            cancelComposition(false)
        }
        lastSelectionEnd = newSelEnd
    }
    private var cachedPreceding = emptyList<String>()
    private var deleteAnchor = -1
    private var deleteLength = 0

    override fun onCreate() {
        super.onCreate()
        org.slashboard.ime.CrashLogger.init(this)
        
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        soundPool?.let {
            soundIos = it.load(this, org.slashboard.ime.R.raw.sound_ios, 1)
            soundMech = it.load(this, org.slashboard.ime.R.raw.sound_mechanical, 1)
            soundType = it.load(this, org.slashboard.ime.R.raw.sound_typewriter, 1)
        prefs = KeyboardPreferences(this)
        
        voiceInputManager = VoiceInputManager(
            context = this,
            onVoiceResult = { text ->
                currentInputConnection?.commitText(text + " ", 1)
                updateSuggestions()
            onError = { error ->
                currentInputConnection?.finishComposingText()
                if (error == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    android.widget.Toast.makeText(this, "Microphone permission required for voice input", android.widget.Toast.LENGTH_SHORT).show()
            onReady = {
                // optional UI indication
                android.widget.Toast.makeText(this, "Listening...", android.widget.Toast.LENGTH_SHORT).show()
            // WorkManager might not be initialized in Robolectric tests.
            e.printStackTrace()
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Safely re-initialize state on focus regain to prevent stale InputConnection usage
        if (!restarting) {
            cancelComposition(false)


    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this); restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)

    override fun onCharacter(value: String) {
        runCatching {
            if (editorLayout != EditorLayout.TEXT || prefs.useEnglish) {
                commitComposition(); currentInputConnection?.commitText(value, 1)
                commitComposition(); currentInputConnection?.commitText(value, 1); if (value.codePoints().anyMatch { it > 0x1F000 }) rememberEmoji(value)
            updateSuggestions()
    override fun onSpace() { runCatching { val word = commitComposition(); currentInputConnection?.commitText(" ", 1); learn(word); precedingDirty = true; updateSuggestions() } }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this)
        restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }

    override fun onFinishInput() {
        deleteAnchor = -1
        deleteLength = 0
        cancelComposition(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        voiceInputManager?.destroy()
        soundPool?.release()
        stopClipboardListener()
        serviceScope.cancel()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            cancelComposition(false)
        }
        lastSelectionEnd = newSelEnd
    }
    override fun onEnter() {
        runCatching {
            val word = commitComposition(); learn(word)
            val info = currentInputEditorInfo
            val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                currentInputConnection?.performEditorAction(action)
                if (action == EditorInfo.IME_ACTION_DONE) requestHideSelf(0)
            cancelComposition(false); updateSuggestions()
    override fun onCandidate(value: String) {
        runCatching {
            feedback()
            if (value == SlashboardEasterEgg.TRUE_NAME_DISPLAY) {
                currentInputConnection?.setComposingText(SlashboardEasterEgg.TRUE_NAME_INSERT, 1); currentInputConnection?.finishComposingText()
                composition.clear(); slsSource.clear(); updateSuggestions(); return
    override fun onCursorDelta(delta: Int) {
        runCatching {
            if (delta == 0) return; commitComposition(); val ic = currentInputConnection ?: return
            val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
            val next = (extracted.selectionEnd + delta).coerceIn(0, extracted.text.length); ic.setSelection(next, next)

    override fun onPressFeedback() {
        if (prefs.keySounds) {
            runCatching {
                when (prefs.soundPack) {
                    "ios" -> soundPool?.play(soundIos, 1f, 1f, 1, 0, 1f)
                    "mechanical" -> soundPool?.play(soundMech, 1f, 1f, 1, 0, 1f)
                    "typewriter" -> soundPool?.play(soundType, 1f, 1f, 1, 0, 1f)
                    else -> (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, .35f)
                // Fallback to default click if sound pool fails
                (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, .35f)
        return prediction?.prefixEvidence(next) ?: 0f
        deleteLength = consumed
        runCatching { ic.setSelection((deleteAnchor - consumed).coerceAtLeast(0), deleteAnchor) }

    override fun onCancelPreviewDelete() {
        val ic = currentInputConnection
        if (ic != null && deleteAnchor >= 0) runCatching { ic.setSelection(deleteAnchor, deleteAnchor) }
        deleteAnchor = -1
        deleteLength = 0
        if (keyCode == KeyEvent.KEYCODE_DEL) { onBackspace(); return true }
        if (keyCode == KeyEvent.KEYCODE_SPACE) { onSpace(); return true }
        if (keyCode == KeyEvent.KEYCODE_ENTER) { onEnter(); return true }
        return super.onKeyDown(keyCode, event)
    private fun clearLocalCompositionState() {
        composition.clear(); slsSource.clear(); generation++; predictionTask?.cancel(true)
        precedingDirty = true
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this)
        restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }

    override fun onFinishInput() {
        deleteAnchor = -1
        deleteLength = 0
        cancelComposition(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        voiceInputManager?.destroy()
        soundPool?.release()
        stopClipboardListener()
        serviceScope.cancel()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            cancelComposition(false)
        }
        lastSelectionEnd = newSelEnd
    }
        if (::keyboard.isInitialized) keyboard.setCandidates(emptyList())
        composition.clear(); slsSource.clear(); generation++; predictionTask?.cancel(true)
        precedingDirty = true
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this)
        restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }

    override fun onFinishInput() {
        deleteAnchor = -1
        deleteLength = 0
        cancelComposition(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        voiceInputManager?.destroy()
        soundPool?.release()
        stopClipboardListener()
        serviceScope.cancel()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            cancelComposition(false)
        }
        lastSelectionEnd = newSelEnd
    }
        if (::keyboard.isInitialized) keyboard.setCandidates(emptyList())
            val cluster = GraphemeDelete.lastCluster(before)
            if (cluster.isEmpty()) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                return
            ic.beginBatchEdit()
            ic.deleteSurroundingText(cluster.length, 0)
            ic.commitText(reduced, 1)
            ic.endBatchEdit()
    private fun updateSuggestions() {
        if (!::keyboard.isInitialized || restricted || !prefs.suggestions) { if (::keyboard.isInitialized) keyboard.setCandidates(emptyList()); return }
        
        val beforeString = runCatching { currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() }.getOrNull().orEmpty()
        val mathMatch = Regex("([0-9]+(?:\\.[0-9]+)?)([\\+\\-\\*\\/])([0-9]+(?:\\.[0-9]+)?)=$").find(beforeString)
        if (mathMatch != null && !composition.active) {
            val a = mathMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val op = mathMatch.groupValues[2]
            val b = mathMatch.groupValues[3].toDoubleOrNull() ?: 0.0
            val res = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else 0.0
                else -> 0.0
        
        if (!composition.active) precedingDirty = true
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this)
        restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }

    override fun onFinishInput() {
        deleteAnchor = -1
        deleteLength = 0
        cancelComposition(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        voiceInputManager?.destroy()
        soundPool?.release()
        stopClipboardListener()
        serviceScope.cancel()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            cancelComposition(false)
        }
        lastSelectionEnd = newSelEnd
    }
        val prefix = composition.rendered; val context = precedingWords(); val token = ++generation
        predictionTask?.cancel(true)
        val currentPrediction = prediction
        if (currentPrediction == null) {
            keyboard.setCandidates(emptyList())
            return
                main.post { if (token == generation) keyboard.setCandidates(values.distinct().take(3)) }
    private fun precedingWords(): List<String> {
        if (!precedingDirty && composition.active) return cachedPreceding
        val before = runCatching { currentInputConnection?.getTextBeforeCursor(256, 0)?.toString() }.getOrNull().orEmpty()
        val withoutComposing = if (composition.rendered.isNotEmpty() && before.endsWith(composition.rendered)) before.dropLast(composition.rendered.length) else before
        cachedPreceding = Regex("[\\p{L}\\p{M}]+").findAll(withoutComposing).map { it.value }.toList().takeLast(2)
        precedingDirty = false
        return cachedPreceding
        keyboard.setOtpAvailable(hasOtp)

    private fun listenForClipboard() {
        runCatching {
            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            manager.removePrimaryClipChangedListener(clipListener)
            if (prefs.clipboardHistory && !restricted && editorLayout == EditorLayout.TEXT) {
                manager.addPrimaryClipChangedListener(clipListener)
        if (prefs.keySounds) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, .35f)

    companion object {
        fun isRestrictedEditor(info: EditorInfo): Boolean {
            val cls = info.inputType and InputType.TYPE_MASK_CLASS
            val variation = info.inputType and InputType.TYPE_MASK_VARIATION
            if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME) return true
            if (cls == InputType.TYPE_CLASS_TEXT && variation in setOf(
                    InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_TEXT_VARIATION_FILTER
                )) return true
            return info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0 ||
                info.imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_SEARCH

        fun editorLayout(info: EditorInfo?): EditorLayout {
            if (info == null) return EditorLayout.TEXT
            val cls = info.inputType and InputType.TYPE_MASK_CLASS
            val variation = info.inputType and InputType.TYPE_MASK_VARIATION
            return when (cls) {
                InputType.TYPE_CLASS_NUMBER -> {
                    val decimal = info.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
                    val signed = info.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
                    when { decimal && signed -> EditorLayout.SIGNED_DECIMAL; decimal -> EditorLayout.DECIMAL; signed -> EditorLayout.SIGNED_NUMBER; else -> EditorLayout.NUMBER }
                else -> EditorLayout.TEXT
