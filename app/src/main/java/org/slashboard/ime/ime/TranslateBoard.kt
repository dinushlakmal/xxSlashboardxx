package org.slashboard.ime.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.*
import org.slashboard.ime.R
import org.slashboard.ime.engine.InputMode
import org.slashboard.ime.engine.SinhalaEngine
import org.slashboard.ime.translator.TranslatorEngine

/**
 * Integrated In-Keyboard Real-Time Translation Bar for Slashboard IME.
 * Appears above the typing keys so users can type naturally, see instant translations,
 * and send/insert the translated result straight into any app (WhatsApp, Messenger, SMS, etc.).
 */
internal class TranslateBoard(
    context: Context,
    private val colors: KeyboardColors,
    private val actions: KeyboardActions,
    private val onDismiss: () -> Unit,
    var onLanguageSwapped: ((useEnglish: Boolean) -> Unit)? = null
) : LinearLayout(context) {

    private val dp = { value: Number ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }

    // Permanent High-Contrast Dark & White Palette (Theme independent)
    private val tbBg = Color.parseColor("#121212")
    private val tbCardBg = Color.parseColor("#1E1E1E")
    private val tbCardStroke = Color.parseColor("#383838")
    private val tbText = Color.WHITE
    private val tbMutedText = Color.parseColor("#B0BEC5")
    private val tbAction = Color.parseColor("#1A73E8")
    private val tbActionText = Color.WHITE
    private val tbChipBg = Color.parseColor("#262626")
    private val tbChipStroke = Color.parseColor("#444444")
    private val tbLiveGreen = Color.parseColor("#81C784")

    var sourceLang = "si" // "si" or "en"
        private set
    var targetLang = "en" // "en" or "si"
        private set

    private var rawInputBuffer = StringBuilder()
    private var lastTranslatedText = ""
    private var translationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Views
    private val langToggleBtn = TextView(context)
    private val inputTextView = TextView(context)
    private val resultTextView = TextView(context)
    private val sendBtn = TextView(context)
    private val copyBtn = ImageView(context)
    private val clearBtn = ImageView(context)
    private val pasteBtn = ImageView(context)
    private val statusIndicator = TextView(context)
    private val phrasesContainer = LinearLayout(context)

    init {
        orientation = VERTICAL
        clipChildren = true
        clipToPadding = true
        setBackgroundColor(tbBg)
        setPadding(dp(8), dp(4), dp(8), dp(4))

        buildHeader()
        buildTranslationRow()
        buildQuickPhrases()
    }

    private fun buildHeader() {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }

        // Title with icon
        val titleIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_key_translate)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#64B5F6"))
            layoutParams = LayoutParams(dp(18), dp(18))
        }
        header.addView(titleIcon)

        val titleText = TextView(context).apply {
            text = " Google Translate"
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tbText)
            setPadding(dp(4), 0, dp(8), 0)
        }
        header.addView(titleText)

        // Language Direction Switcher Pill
        updateLangButtonText()
        langToggleBtn.apply {
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tbActionText)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(3), dp(10), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(tbAction)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                swapLanguages()
            }
        }
        header.addView(langToggleBtn)

        val spacer = Space(context)
        header.addView(spacer, LayoutParams(0, dp(1), 1f))

        // Status
        statusIndicator.apply {
            text = "Live"
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tbLiveGreen)
            setPadding(0, 0, dp(6), 0)
        }
        header.addView(statusIndicator)

        // Close button (✖)
        val closeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(tbText)
            background = rippleBg(tbText)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            isClickable = true
            isFocusable = true
            setOnClickListener { onDismiss() }
        }
        header.addView(closeBtn, LayoutParams(dp(24), dp(24)))

        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun buildTranslationRow() {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        // Card containing Input + Output in a clean split box
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(tbCardBg)
                setStroke(dp(1), tbCardStroke)
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        // Input view (shows what is typed)
        inputTextView.apply {
            text = if (sourceLang == "si") "කීබෝඩ් එකෙන් ටයිප් කරන්න..." else "Type on keyboard to translate..."
            textSize = 12f
            setTextColor(tbMutedText)
            maxLines = 1
            setSingleLine(true)
        }
        card.addView(inputTextView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Translated Result View
        resultTextView.apply {
            text = "පරිවර්තනය (Translation result)"
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tbText)
            maxLines = 1
            setSingleLine(true)
        }
        card.addView(resultTextView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(2)
        })

        row.addView(card, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        // Clear Button
        clearBtn.apply {
            setImageResource(android.R.drawable.ic_notification_clear_all)
            imageTintList = ColorStateList.valueOf(tbMutedText)
            background = rippleBg(tbText)
            visibility = View.GONE
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener {
                clearInput()
            }
        }
        row.addView(clearBtn, LayoutParams(dp(28), dp(28)).apply { marginStart = dp(2) })

        // Copy Button
        copyBtn.apply {
            setImageResource(R.drawable.ic_key_clipboard)
            imageTintList = ColorStateList.valueOf(tbText)
            background = rippleBg(tbText)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener {
                if (lastTranslatedText.isNotEmpty()) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Translation", lastTranslatedText))
                    Toast.makeText(context, "පිටපත් කරගන්නා ලදී (Copied)", Toast.LENGTH_SHORT).show()
                }
            }
        }
        row.addView(copyBtn, LayoutParams(dp(28), dp(28)).apply { marginStart = dp(2) })

        // Send / Commit Button (Directly inserts/sends into active chat/app!)
        sendBtn.apply {
            text = "සෙන්ඩ් ↵"
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tbActionText)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(tbAction)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                commitAndSend()
            }
        }
        row.addView(sendBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32)).apply {
            marginStart = dp(4)
        })

        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun buildQuickPhrases() {
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
        }

        phrasesContainer.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(1))
        }
        scroll.addView(phrasesContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        updateQuickPhrases()
    }

    private fun updateQuickPhrases() {
        phrasesContainer.removeAllViews()
        val phrases = if (sourceLang == "si") {
            listOf("ආයුබෝවන්", "සුබ උදෑසනක්", "ස්තූතියි", "සමාවෙන්න", "ඔයාට කොහොමද?", "මම හොඳින්", "මේක කීයද?", "උදව් කරන්න")
        } else {
            listOf("Hello", "Good morning", "Thank you", "Sorry", "How are you?", "I am fine", "How much?", "Help me")
        }

        phrases.forEach { phrase ->
            val chip = TextView(context).apply {
                text = phrase
                textSize = 11.5f
                setTextColor(tbText)
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(tbChipBg)
                    setStroke(dp(1), tbChipStroke)
                }
                setPadding(dp(8), dp(3), dp(8), dp(3))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    rawInputBuffer.clear()
                    rawInputBuffer.append(phrase)
                    onInputUpdated()
                }
            }
            phrasesContainer.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(4)
            })
        }
    }

    private fun updateLangButtonText() {
        if (sourceLang == "si") {
            langToggleBtn.text = "සිංහල ➔ English ⇄"
        } else {
            langToggleBtn.text = "English ➔ සිංහල ⇄"
        }
    }

    fun swapLanguages() {
        val temp = sourceLang
        sourceLang = targetLang
        targetLang = temp
        updateLangButtonText()

        val oldResult = lastTranslatedText
        if (oldResult.isNotEmpty() && !oldResult.startsWith("පරිවර්තනය") && !oldResult.startsWith("Translating")) {
            rawInputBuffer.clear()
            rawInputBuffer.append(oldResult)
        } else {
            rawInputBuffer.clear()
        }

        onLanguageSwapped?.invoke(sourceLang == "en")
        updateQuickPhrases()
        onInputUpdated()
    }

    /**
     * Called when a key is pressed on the keyboard underneath.
     */
    fun appendInput(charString: String) {
        rawInputBuffer.append(charString)
        onInputUpdated()
    }

    /**
     * Called when backspace is pressed.
     */
    fun backspace(word: Boolean = false) {
        if (rawInputBuffer.isEmpty()) return
        if (word) {
            val idx = rawInputBuffer.lastIndexOf(' ')
            if (idx != -1) {
                rawInputBuffer.delete(idx, rawInputBuffer.length)
            } else {
                rawInputBuffer.clear()
            }
        } else {
            rawInputBuffer.deleteCharAt(rawInputBuffer.length - 1)
        }
        onInputUpdated()
    }

    fun clearInput() {
        rawInputBuffer.clear()
        lastTranslatedText = ""
        inputTextView.text = if (sourceLang == "si") "කීබෝඩ් එකෙන් ටයිප් කරන්න..." else "Type on keyboard to translate..."
        resultTextView.text = "පරිවර්තනය (Translation result)"
        clearBtn.visibility = View.GONE
    }

    /**
     * Commits/sends the translated text into the active app input field and resets buffer.
     */
    fun commitAndSend() {
        val toSend = if (lastTranslatedText.isNotBlank()) lastTranslatedText else rawInputBuffer.toString()
        if (toSend.isNotBlank()) {
            actions.onCharacter(toSend + " ")
            clearInput()
        }
    }

    private fun onInputUpdated() {
        val raw = rawInputBuffer.toString()
        if (raw.isEmpty()) {
            clearInput()
            return
        }

        clearBtn.visibility = View.VISIBLE

        // If source is Sinhala, display typed/transliterated text
        val displayText = if (sourceLang == "si" && !TranslatorEngine.isSinhala(raw)) {
            SinhalaEngine.transliterate(raw, InputMode.SMART_PHONETIC)
        } else {
            raw
        }
        inputTextView.text = displayText

        // Perform debounced translation
        performTranslation(displayText)
    }

    private fun performTranslation(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            resultTextView.text = "පරිවර්තනය"
            lastTranslatedText = ""
            return
        }

        translationJob?.cancel()
        resultTextView.text = "පරිවර්තනය වෙමින් පවතී..."

        translationJob = scope.launch {
            delay(160) // Fast 160ms debounce for responsive live typing
            val result = TranslatorEngine.translate(trimmed, sourceLang, targetLang)
            if (isActive) {
                if (result.isSuccess) {
                    val text = result.getOrNull().orEmpty()
                    lastTranslatedText = text
                    resultTextView.text = text
                } else {
                    lastTranslatedText = trimmed
                    resultTextView.text = trimmed
                }
            }
        }
    }

    private fun rippleBg(inkColor: Int): RippleDrawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
        }
        val content = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
        }
        return RippleDrawable(
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(inkColor, 80)),
            content,
            mask
        )
    }

    fun release() {
        scope.cancel()
    }
}
