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

class SlashboardInputMethodService : InputMethodService(), KeyboardActions {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefs: KeyboardPreferences
    private lateinit var keyboard: KeyboardView
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

    private var precedingDirty = true
    private var cachedPreceding = emptyList<String>()
    private var deleteAnchor = -1
    private var deleteLength = 0

    override fun onCreate() {
        super.onCreate()
        prefs = KeyboardPreferences(this)
        
        // Schedule background worker for periodic layout caching and dictionary trimming
        try {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<org.slashboard.ime.data.SlashboardSyncWorker>(
                1, java.util.concurrent.TimeUnit.DAYS
            ).build()
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SlashboardBackgroundSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            // One-off request to seed the cache initially
            val initialRequest = androidx.work.OneTimeWorkRequestBuilder<org.slashboard.ime.data.SlashboardSyncWorker>().build()
            androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
                "SlashboardInitialSync",
                androidx.work.ExistingWorkPolicy.KEEP,
                initialRequest
            )
        } catch (e: Exception) {
            // WorkManager might not be initialized in Robolectric tests.
            e.printStackTrace()
        }

        // Offload heavy data repositories and model warmup into a CoroutineScope
        // to keep the main/UI thread free and reduce keyboard launch latency
        serviceScope.launch(Dispatchers.IO) {
            val localLearning = LocalLearningStore(this@SlashboardInputMethodService)
            val predictionRepo = PredictionRepository(this@SlashboardInputMethodService, localLearning)
            val emojiRepo = EmojiRepository(this@SlashboardInputMethodService)
            val clipboardStore = ClipboardHistoryStore(this@SlashboardInputMethodService)
            predictionRepo.warmup()

            learning = localLearning
            prediction = predictionRepo
            emoji = emojiRepo
            clipboardHistory = clipboardStore

            withContext(Dispatchers.Main) {
                if (::keyboard.isInitialized) {
                    keyboard.updateRepositories(emojiRepo, clipboardStore)
                    keyboard.setClipboardItems(clipboardStore.items(), clipboardStore.pinnedItems())
                    updateSuggestions()
                }
            }
        }
    }
    override fun onCreateInputView(): View {
        keyboard = KeyboardView(this, this, prefs, emoji, clipboardHistory)
        return keyboard
    }
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting); cancelComposition(false)
        restricted = attribute?.let(::isRestrictedEditor) ?: true
        lastSelectionEnd = attribute?.initialSelEnd ?: -1
        precedingDirty = true
    }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this); restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }
    override fun onFinishInput() { deleteAnchor = -1; deleteLength = 0; cancelComposition(false); super.onFinishInput() }
    override fun onDestroy() {
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
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) cancelComposition(false)
        lastSelectionEnd = newSelEnd
    }

    override fun onCharacter(value: String) {
        runCatching {
            if (editorLayout != EditorLayout.TEXT || prefs.useEnglish) {
                commitComposition(); currentInputConnection?.commitText(value, 1)
            } else if (prefs.mode == InputMode.WIJESEKARA && (value == "\u200D" || value.codePoints().anyMatch { it in 0x0D80..0xE0FF })) {
                slsSource.append(value)
                val rendered = SinhalaEngine.normalizeSls(slsSource.toString())
                composition.replace(rendered); currentInputConnection?.setComposingText(rendered, 1)
            } else if (prefs.mode != InputMode.WIJESEKARA && value.length == 1 && value[0].isLetter() && value[0].code < 128) {
                val rendered = composition.type(value, prefs.mode)
                currentInputConnection?.setComposingText(rendered, 1)
            } else {
                commitComposition(); currentInputConnection?.commitText(value, 1); if (value.codePoints().anyMatch { it > 0x1F000 }) rememberEmoji(value)
            }
            updateSuggestions()
        }
    }

    override fun onBackspace(word: Boolean) {
        runCatching {
            if (composition.active) {
                if (prefs.mode == InputMode.WIJESEKARA) {
                    if (slsSource.isNotEmpty()) {
                        val next = GraphemeDelete.peelLastScalar(slsSource.toString())
                        slsSource.setLength(0)
                        slsSource.append(next)
                    } else {
                        slsSource.append(GraphemeDelete.reduceSlashboard(composition.rendered).orEmpty())
                    }
                    val rendered = SinhalaEngine.normalizeSls(slsSource.toString())
                    composition.replace(rendered)
                    if (rendered.isEmpty()) {
                        currentInputConnection?.setComposingText("", 1)
                        currentInputConnection?.finishComposingText()
                        slsSource.clear()
                    } else {
                        currentInputConnection?.setComposingText(rendered, 1)
                    }
                } else {
                    val rendered = composition.backspace(prefs.mode)
                    if (rendered.isEmpty()) {
                        currentInputConnection?.setComposingText("", 1)
                        currentInputConnection?.finishComposingText()
                    } else currentInputConnection?.setComposingText(rendered, 1)
                }
            } else {
                deleteFromHost(word)
                precedingDirty = true
            }
            updateSuggestions()
        }
    }
    override fun onSpace() { runCatching { val word = commitComposition(); currentInputConnection?.commitText(" ", 1); learn(word); precedingDirty = true; updateSuggestions() } }
    override fun onEnter() {
        runCatching {
            val word = commitComposition(); learn(word)
            val info = currentInputEditorInfo
            val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                currentInputConnection?.performEditorAction(action)
                if (action == EditorInfo.IME_ACTION_DONE) requestHideSelf(0)
            } else {
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)).also { currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) }
            }
            cancelComposition(false); updateSuggestions()
        }
    }
    override fun onCandidate(value: String) {
        runCatching {
            feedback()
            if (value == SlashboardEasterEgg.TRUE_NAME_DISPLAY) {
                currentInputConnection?.setComposingText(SlashboardEasterEgg.TRUE_NAME_INSERT, 1); currentInputConnection?.finishComposingText()
                composition.clear(); slsSource.clear(); updateSuggestions(); return
            }
            currentInputConnection?.setComposingText(value, 1); currentInputConnection?.finishComposingText()
            composition.clear(); slsSource.clear(); learn(value); currentInputConnection?.commitText(" ", 1)
            precedingDirty = true
            updateSuggestions()
        }
    }
    override fun onGlobe() { runCatching { commitComposition(); switchSystemKeyboard() } }
    override fun onModeRequested(mode: InputMode) { runCatching { commitComposition(); prefs.mode = mode; keyboard.configure(mode, offerSystemSwitch(), enterLabel(currentInputEditorInfo), editorLayout) } }
    override fun onHide() { runCatching { commitComposition(); requestHideSelf(0) } }
    override fun onCursorDelta(delta: Int) {
        runCatching {
            if (delta == 0) return; commitComposition(); val ic = currentInputConnection ?: return
            val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
            val next = (extracted.selectionEnd + delta).coerceIn(0, extracted.text.length); ic.setSelection(next, next)
        }
    }

    override fun onPressFeedback() {
        if (prefs.keySounds) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, .35f)
    }

    override fun languageScoreForKey(output: String): Float {
        if (restricted || editorLayout != EditorLayout.TEXT) return 0f
        val next = if (prefs.mode != InputMode.WIJESEKARA && output.length == 1 && output[0].isLetter() && output[0].code < 128) {
            SinhalaEngine.transliterate(composition.source + output, prefs.mode)
        } else if (prefs.mode == InputMode.WIJESEKARA) {
            SinhalaEngine.normalizeSls(slsSource.toString() + output)
        } else return 0f
        return prediction?.prefixEvidence(next) ?: 0f
    }

    override fun onPreviewDelete(clusters: Int) {
        val ic = currentInputConnection ?: return
        commitComposition()
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
        if (deleteAnchor < 0) deleteAnchor = extracted.selectionEnd
        val before = ic.getTextBeforeCursor(256, 0)?.toString().orEmpty()
        var remaining = clusters
        var consumed = 0
        var text = before
        while (remaining > 0 && text.isNotEmpty()) {
            val cluster = GraphemeDelete.lastCluster(text)
            if (cluster.isEmpty()) break
            consumed += cluster.length
            text = text.dropLast(cluster.length)
            remaining--
        }
        deleteLength = consumed
        runCatching { ic.setSelection((deleteAnchor - consumed).coerceAtLeast(0), deleteAnchor) }
    }

    override fun onCommitPreviewDelete() {
        val ic = currentInputConnection
        if (ic != null && deleteLength > 0) ic.commitText("", 1)
        deleteAnchor = -1
        deleteLength = 0
        precedingDirty = true
        updateSuggestions()
    }

    override fun onCancelPreviewDelete() {
        val ic = currentInputConnection
        if (ic != null && deleteAnchor >= 0) runCatching { ic.setSelection(deleteAnchor, deleteAnchor) }
        deleteAnchor = -1
        deleteLength = 0
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.isPrintingKey && !event.isCtrlPressed && !event.isAltPressed) {
            onCharacter(event.unicodeChar.toChar().toString()); return true
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) { onBackspace(); return true }
        if (keyCode == KeyEvent.KEYCODE_SPACE) { onSpace(); return true }
        if (keyCode == KeyEvent.KEYCODE_ENTER) { onEnter(); return true }
        return super.onKeyDown(keyCode, event)
    }

    private fun commitComposition(): String? {
        if (!composition.active) return null
        val word = composition.rendered.takeIf { it.isNotBlank() }
        currentInputConnection?.finishComposingText(); composition.clear(); slsSource.clear(); generation++
        return word
    }
    private fun cancelComposition(removeHostText: Boolean) {
        if (removeHostText && composition.rendered.isNotEmpty()) currentInputConnection?.deleteSurroundingText(composition.rendered.length, 0)
        currentInputConnection?.finishComposingText(); composition.clear(); slsSource.clear(); generation++; predictionTask?.cancel(true)
        precedingDirty = true
        if (::keyboard.isInitialized) keyboard.setCandidates(emptyList())
    }
    private fun deleteFromHost(word: Boolean) {
        val ic = currentInputConnection ?: return
        runCatching {
            val before = ic.getTextBeforeCursor(if (word) 256 else 32, 0)?.toString().orEmpty()
            if (word) {
                val target = GraphemeDelete.lastWordSegment(before)
                if (target.isNotEmpty()) ic.deleteSurroundingText(target.length, 0)
                else ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                return
            }
            val cluster = GraphemeDelete.lastCluster(before)
            if (cluster.isEmpty()) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                return
            }
            val reduced = GraphemeDelete.reduceSlashboard(cluster)
            if (reduced == null) {
                ic.deleteSurroundingText(cluster.length, 0)
                return
            }
            ic.beginBatchEdit()
            ic.deleteSurroundingText(cluster.length, 0)
            ic.commitText(reduced, 1)
            ic.endBatchEdit()
        }
    }
    private fun updateSuggestions() {
        if (!::keyboard.isInitialized || restricted || !prefs.suggestions) { if (::keyboard.isInitialized) keyboard.setCandidates(emptyList()); return }
        if (!composition.active) precedingDirty = true
        val prefix = composition.rendered; val context = precedingWords(); val token = ++generation
        predictionTask?.cancel(true)
        val currentPrediction = prediction
        if (currentPrediction == null) {
            keyboard.setCandidates(emptyList())
            return
        }
        val currentEmoji = emoji
        predictionTask = executor.submit {
            try {
                val values = currentPrediction.candidates(prefix, context, 3).map { it.text }.toMutableList()
                if (SlashboardEasterEgg.isCompleteTrueName(prefix, composition.source)) values.add(0, SlashboardEasterEgg.TRUE_NAME_DISPLAY)
                val emojis = if (prefs.emojiSuggestions && prefix.isNotBlank() && currentEmoji != null) currentEmoji.search(prefix, 2, scanNames = false) else emptyList()
                if (emojis.isNotEmpty()) {
                    if (values.isNotEmpty()) values.add(minOf(1, values.size), emojis.first())
                    else values.add(emojis.first())
                }
                main.post { if (token == generation) keyboard.setCandidates(values.distinct().take(3)) }
            } catch (_: Throwable) {
                main.post { if (token == generation) keyboard.setCandidates(emptyList()) }
            }
        }
    }
    private fun precedingWords(): List<String> {
        if (!precedingDirty && composition.active) return cachedPreceding
        val before = runCatching { currentInputConnection?.getTextBeforeCursor(256, 0)?.toString() }.getOrNull().orEmpty()
        val withoutComposing = if (composition.rendered.isNotEmpty() && before.endsWith(composition.rendered)) before.dropLast(composition.rendered.length) else before
        cachedPreceding = Regex("[\\p{L}\\p{M}]+").findAll(withoutComposing).map { it.value }.toList().takeLast(2)
        precedingDirty = false
        return cachedPreceding
    }
    private fun learn(word: String?) {
        if (word.isNullOrBlank() || restricted) return
        val previous = previousCommittedWord
        previousCommittedWord = word
        learning?.let { store ->
            executor.submit { store.record(word, previous) }
        }
    }
    private fun captureClipboard() {
        runCatching {
            if (!prefs.clipboardHistory || restricted || editorLayout != EditorLayout.TEXT) return
            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val clip = manager.primaryClip ?: return
            if (clip.itemCount == 0) return
            val store = clipboardHistory ?: return
            clip.getItemAt(0).coerceToText(this)?.toString()?.let(store::add)
        }
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureClipboard()
        if (::keyboard.isInitialized) clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
    }

    private fun listenForClipboard() {
        runCatching {
            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            manager.removePrimaryClipChangedListener(clipListener)
            if (prefs.clipboardHistory && !restricted && editorLayout == EditorLayout.TEXT) {
                manager.addPrimaryClipChangedListener(clipListener)
            }
        }
    }

    private fun stopClipboardListener() {
        runCatching {
            (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.removePrimaryClipChangedListener(clipListener)
        }
    }
    private fun rememberEmoji(value: String) { recentEmoji.remove(value); recentEmoji.add(0, value); if (recentEmoji.size > 32) recentEmoji = recentEmoji.take(32).toMutableList(); keyboard.setRecentEmoji(recentEmoji) }
    private fun feedback() {
        if (prefs.haptics && ::keyboard.isInitialized) {
            val type = if (Build.VERSION.SDK_INT >= 27) android.view.HapticFeedbackConstants.KEYBOARD_PRESS else android.view.HapticFeedbackConstants.KEYBOARD_TAP
            keyboard.performHapticFeedback(type)
        }
        if (prefs.keySounds) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, .35f)
    }
    private fun offerSystemSwitch(): Boolean = if (Build.VERSION.SDK_INT >= 28) shouldOfferSwitchingToNextInputMethod()
        else (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).shouldOfferSwitchingToNextInputMethod(window.window?.attributes?.token)
    private fun switchSystemKeyboard() {
        if (Build.VERSION.SDK_INT >= 28) switchToNextInputMethod(false)
        else (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).switchToNextInputMethod(window.window?.attributes?.token, false)
    }

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
        }
        fun enterLabel(info: EditorInfo?): String = when (info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_GO -> "Go"; EditorInfo.IME_ACTION_SEARCH -> "⌕"; EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"; EditorInfo.IME_ACTION_DONE -> "Done"; else -> "↵"
        }

        fun editorLayout(info: EditorInfo?): EditorLayout {
            if (info == null) return EditorLayout.TEXT
            val cls = info.inputType and InputType.TYPE_MASK_CLASS
            val variation = info.inputType and InputType.TYPE_MASK_VARIATION
            return when (cls) {
                InputType.TYPE_CLASS_NUMBER -> {
                    val decimal = info.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
                    val signed = info.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
                    when { decimal && signed -> EditorLayout.SIGNED_DECIMAL; decimal -> EditorLayout.DECIMAL; signed -> EditorLayout.SIGNED_NUMBER; else -> EditorLayout.NUMBER }
                }
                InputType.TYPE_CLASS_PHONE -> EditorLayout.PHONE
                InputType.TYPE_CLASS_DATETIME -> EditorLayout.DATETIME
                InputType.TYPE_CLASS_TEXT -> when (variation) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> EditorLayout.EMAIL
                    InputType.TYPE_TEXT_VARIATION_URI -> EditorLayout.URI
                    InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> EditorLayout.ASCII
                    else -> EditorLayout.TEXT
                }
                else -> EditorLayout.TEXT
            }
        }
    }
}
