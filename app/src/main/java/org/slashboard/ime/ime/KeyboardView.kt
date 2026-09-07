package org.slashboard.ime.ime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.Uri
import org.slashboard.ime.BuildConfig
import org.slashboard.ime.data.ClipboardHistoryStore
import org.slashboard.ime.data.EmojiRepository
import org.slashboard.ime.engine.InputMode
import org.slashboard.ime.engine.SinhalaEngine
import org.slashboard.ime.settings.KeyboardPreferences
import kotlin.math.abs

enum class KeyboardLayer { LETTERS, NUMBERS, SYMBOLS, SINHALA_GLYPHS, EMOJI, CLIPBOARD, VOICE, TRANSLATE }
enum class EditorLayout { TEXT, ASCII, EMAIL, URI, NUMBER, SIGNED_NUMBER, DECIMAL, SIGNED_DECIMAL, PHONE, DATETIME }

interface KeyboardActions {
    fun onCharacter(value: String)
    fun onBackspace(word: Boolean = false)
    fun onSpace()
    fun onEnter()
    fun onCandidate(value: String)
    fun onGlobe()
    fun onModeRequested(mode: InputMode)
    fun onHide()
    fun onVoiceInputRequested() {}
    fun onCursorDelta(delta: Int)
    fun onPressFeedback() {}
    fun languageScoreForKey(output: String): Float = 0f
    fun onPreviewDelete(clusters: Int) {}
    fun onCommitPreviewDelete() {}
    fun onCancelPreviewDelete() {}
    fun onToolbarAction(action: String) {}
    fun onMediaSelected(uri: Uri, mimeType: String, description: String = "Media") {}
}

@SuppressLint("ViewConstructor")
class KeyboardView(
    context: Context,
    private val actions: KeyboardActions,
    private val prefs: KeyboardPreferences,
    emojiRepository: EmojiRepository? = null,
    clipboardHistoryStore: ClipboardHistoryStore? = null
) : LinearLayout(context) {
    private var mode = prefs.mode
    private var layer = KeyboardLayer.LETTERS
    private val shiftLatch = ShiftLatch()
    private val shifted get() = shiftLatch.shifted || shiftLatch.capsLock
    private val capsLock get() = shiftLatch.capsLock
    private var enterLabel = "↵"
    private var editorLayout = EditorLayout.TEXT
    private var offerGlobe = false
    private var animateSpaceLabel = true
    private var candidates = emptyList<String>()
    private var correctionItems = emptySet<String>()
    private var clipboardRecent = emptyList<String>()
    private var clipboardPinned = emptyList<String>()
    private var recentEmoji = emptyList<String>()
    private var emojiRepo: EmojiRepository = emojiRepository ?: EmojiRepository(context)
    private var clipboardStore: ClipboardHistoryStore = clipboardHistoryStore ?: ClipboardHistoryStore(context)
    private var emojiSearch = false
    private var emojiQuery = ""
    private var emojiCategoryIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var palette = KeyboardPaletteResolver.resolve(context, prefs.theme, prefs.highContrast)
    private var bg = palette.background
    private var key = palette.key
    private var utility = palette.utility
    var action = palette.action
    var actionText = palette.actionText
    private var ink = palette.ink
    private var voiceManager: VoiceInputManager? = null
    private val rail = SuggestionRail(
        context, ink, 
        onCandidate = { actions.onCandidate(it) }, 
        onClipboard = { layer = KeyboardLayer.CLIPBOARD; render() },
        onSettings = { 
            val intent = android.content.Intent(context, org.slashboard.ime.settings.SettingsActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        },
        onEmoji = {
            layer = KeyboardLayer.EMOJI
            render()
        },
        onVoice = {
            openVoiceTyping()
            actions.onVoiceInputRequested()
        }
    )
    private val body = LinearLayout(context)
    private val homePad = View(context)
    private val popups = KeyPopups(context)
    private var activeTranslateBoard: TranslateBoard? = null
    private val panel = KeyboardPanel(
        context, prefs, popups, object : KeyboardActions {
            override fun onCharacter(value: String) {
                if (layer == KeyboardLayer.TRANSLATE && activeTranslateBoard != null) {
                    activeTranslateBoard?.appendInput(value)
                } else {
                    actions.onCharacter(value)
                }
                if (shiftLatch.shifted && !shiftLatch.capsLock) {
                    shiftLatch.consumeOneShot()
                    bindTyping()
                }
            }
            override fun onBackspace(word: Boolean) {
                if (layer == KeyboardLayer.TRANSLATE && activeTranslateBoard != null) {
                    activeTranslateBoard?.backspace(word)
                } else {
                    actions.onBackspace(word)
                }
            }
            override fun onSpace() {
                if (layer == KeyboardLayer.TRANSLATE && activeTranslateBoard != null) {
                    activeTranslateBoard?.appendInput(" ")
                } else {
                    actions.onSpace()
                }
            }
            override fun onEnter() {
                if (layer == KeyboardLayer.TRANSLATE && activeTranslateBoard != null) {
                    activeTranslateBoard?.commitAndSend()
                } else {
                    actions.onEnter()
                }
            }
            override fun onCandidate(value: String) {
                if (layer == KeyboardLayer.TRANSLATE && activeTranslateBoard != null) {
                    activeTranslateBoard?.appendInput(value)
                } else {
                    actions.onCandidate(value)
                }
            }
            override fun onGlobe() {
                if (layer == KeyboardLayer.TRANSLATE && activeTranslateBoard != null) {
                    activeTranslateBoard?.swapLanguages()
                } else {
                    prefs.useEnglish = !prefs.useEnglish
                    actions.onGlobe()
                    render()
                }
            }
            override fun onModeRequested(mode: InputMode) = actions.onModeRequested(mode)
            override fun onHide() = actions.onHide()
            override fun onCursorDelta(delta: Int) = actions.onCursorDelta(delta)
            override fun onPressFeedback() = actions.onPressFeedback()
            override fun languageScoreForKey(output: String) = actions.languageScoreForKey(output)
            override fun onPreviewDelete(clusters: Int) = actions.onPreviewDelete(clusters)
            override fun onCommitPreviewDelete() = actions.onCommitPreviewDelete()
            override fun onCancelPreviewDelete() = actions.onCancelPreviewDelete()
        },
        KeyboardColors(key, utility, ink, palette.action, palette.actionText, palette.dark, palette.highContrast, palette.keyRadiusDp, palette.keyOpacity),
        onLayer = { next -> layer = next; render() },
        onShift = { updateShift() }
    )
    var forceNormalKeyboard: Boolean = false
    private var sliverPanel: KeyboardPanel? = null
    var learningEnabled = true
        set(value) {
            field = value
            panel.learningEnabled = value && editorLayout == EditorLayout.TEXT
        }

    init {
        orientation = VERTICAL; setBackgroundColor(bg)
        clipChildren = false
        clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val types = WindowInsetsCompat.Type.navigationBars() or
                WindowInsetsCompat.Type.mandatorySystemGestures() or
                WindowInsetsCompat.Type.tappableElement()
            val system = insets.getInsetsIgnoringVisibility(types).bottom
            val resource = navigationBarFallback()
            val bottom = maxOf(system, resource, dp(KeyboardGeometry.BOTTOM_PAD_DP)).coerceAtMost(dp(64))
            val params = homePad.layoutParams as LayoutParams
            if (params.height != bottom) {
                params.height = bottom
                homePad.layoutParams = params
            }
            insets
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setPadding(0, dp(KeyboardGeometry.TOP_PAD_DP), 0, 0)
        rail.keySliver = suggestionKeySliver()
        rail.onLangToggle = {
            prefs.useEnglish = !prefs.useEnglish
            actions.onGlobe()
            render()
        }
        rail.onToolbarAction = {
            if (it == "astrology") {
                layer = KeyboardLayer.SINHALA_GLYPHS
                render()
            } else {
                actions.onToolbarAction(it)
            }
        }
        rail.onEmojiSelected = { emoji ->
            actions.onCharacter(emoji)
        }
        rail.setRecentEmojis(prefs.recentEmojis)
        addView(rail, LayoutParams(LayoutParams.MATCH_PARENT, suggestionRailHeight()))
        body.orientation = VERTICAL
        body.clipChildren = true
        body.clipToPadding = true
        addView(body, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(homePad, LayoutParams(LayoutParams.MATCH_PARENT, dp(KeyboardGeometry.BOTTOM_PAD_DP)))
        render()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
    }

    override fun onDetachedFromWindow() {
        popups.dismiss()
        handler.removeCallbacksAndMessages(null)
        sliverPanel = null
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return runCatching {
            super.dispatchTouchEvent(event)
        }.getOrDefault(true)
    }

    fun applyTheme() {
        palette = KeyboardPaletteResolver.resolve(context, prefs.theme, prefs.highContrast)
        bg = palette.background
        key = palette.key
        utility = palette.utility
        action = palette.action
        actionText = palette.actionText
        ink = palette.ink
        setBackgroundColor(bg)
                        if (palette.backgroundImagePath != null) {
            try {
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeFile(palette.backgroundImagePath, options)
                var sampleSize = 1
                val maxDim = 1200
                while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                options.inJustDecodeBounds = false
                options.inSampleSize = sampleSize
                val bitmap = android.graphics.BitmapFactory.decodeFile(palette.backgroundImagePath, options)
                if (bitmap != null) {
                    if (palette.blurEffect && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val blurDrawable = object : android.graphics.drawable.Drawable() {
                            private val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(50f, 50f, android.graphics.Shader.TileMode.CLAMP))
                            }
                            private val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                            private val dst = android.graphics.Rect()
                            override fun draw(canvas: android.graphics.Canvas) {
                                dst.set(bounds)
                                canvas.drawBitmap(bitmap, src, dst, paint)
                                canvas.drawColor(if (palette.dark) android.graphics.Color.argb(155, 0, 0, 0) else android.graphics.Color.argb(75, 255, 255, 255))
                            }
                            override fun setAlpha(alpha: Int) {}
                            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
                        }
                        background = blurDrawable
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            setRenderEffect(null)
                        }
                    } else {
                        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap).apply {
                            alpha = if (palette.dark) 100 else 180
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            setRenderEffect(null)
                        }
                        background = drawable
                    }
                }
            } catch (e: Throwable) { e.printStackTrace() }
        } else {
            background = null
        }
        val colors = KeyboardColors(
            key = key,
            utility = utility,
            ink = ink,
            action = action,
            actionText = actionText,
            dark = palette.dark,
            highContrast = palette.highContrast,
            keyRadiusDp = palette.keyRadiusDp,
            keyOpacity = palette.keyOpacity,
            spaceBarKey = palette.spaceKey,
            spaceBarBorder = palette.spaceBorder
        )
        panel.updateColors(colors)
        rail.updateInk(ink)
    }

    fun configure(mode: InputMode, offerGlobe: Boolean, enter: String, editor: EditorLayout = EditorLayout.TEXT) {
        applyTheme()
        forceNormalKeyboard = false
        this.mode = mode; enterLabel = enter; editorLayout = editor; this.offerGlobe = offerGlobe
        shiftLatch.reset(); layer = KeyboardLayer.LETTERS
        animateSpaceLabel = true
        rail.configureToolbar(prefs)
        val width = if (prefs.oneHanded == "center") LayoutParams.MATCH_PARENT else (resources.displayMetrics.widthPixels * .82f).toInt()
        (body.layoutParams as LayoutParams).apply { this.width = width; gravity = when (prefs.oneHanded) { "left" -> Gravity.START; "right" -> Gravity.END; else -> Gravity.CENTER } }
        panel.learningEnabled = learningEnabled && editor == EditorLayout.TEXT
        render()
    }
    fun setCandidates(values: List<String>, corrections: Set<String> = emptySet()) {
        candidates = values.take(3)
        correctionItems = corrections
        bindRail(true)
    }
    fun setOtpAvailable(available: Boolean) {
        rail.setOtpAvailable(available)
    }
    fun setClipboardItems(recent: List<String>, pinned: List<String> = emptyList()) {
        clipboardRecent = recent
        clipboardPinned = pinned
        rail.setClipboardVisible(showClipboardButton())
        if (layer == KeyboardLayer.CLIPBOARD) render()
    }
    fun setRecentEmoji(values: List<String>) {
        recentEmoji = values
        rail.setRecentEmojis(values)
        if (layer == KeyboardLayer.EMOJI) render()
        else if (layer == KeyboardLayer.LETTERS && (prefs.topRow == "emoji" || prefs.topRow == "both")) {
            bindTyping()
        }
    }
    fun updateRepositories(emojiRepository: EmojiRepository, clipboardHistoryStore: ClipboardHistoryStore) {
        emojiRepo = emojiRepository
        clipboardStore = clipboardHistoryStore
    }

    internal fun keyNameAt(x: Float, y: Float): String? {
        if (!usesTypingPanel() || panel.parent !== body) return null
        return panel.keyAt(x - body.left - panel.left, y - body.top - panel.top)?.id
    }

    internal fun typingLayout(): KeyboardLayout? = if (usesTypingPanel()) panel.layout else null

    private fun standardContentHeight(): Int {
        val rows = KeyboardLayoutFactory.typingRows(
            mode, KeyboardLayer.LETTERS, false, false, editorLayout, prefs.topRow, prefs.emojiPicker, enterLabel, " ", false, prefs.useEnglish, prefs.smartKeyModifiers,
            recentEmoji.ifEmpty { prefs.recentEmojis }
        )
        val rowHeight = KeyboardGeometry.rowHeightPx(prefs.keyboardSize, isLandscape(), resources.displayMetrics.density, rows.size)
        val total = rows.sumOf { (rowHeight * it.heightFactor).toDouble() }.toInt()
        return total.coerceAtLeast(dp(220))
    }

    private fun render() {
        popups.dismiss()
        sliverPanel = null
        rail.layoutParams = (rail.layoutParams as LayoutParams).apply {
            height = if (keepSuggestionRail()) suggestionRailHeight() else 0
        }
        bindRail(false)
        if (!forceNormalKeyboard && editorLayout in numericEditors) {
            body.removeAllViews()
            renderNativePad()
            return
        }
        when (layer) {
            KeyboardLayer.LETTERS, KeyboardLayer.NUMBERS, KeyboardLayer.SYMBOLS, KeyboardLayer.SINHALA_GLYPHS -> bindTyping()
            KeyboardLayer.EMOJI -> { body.removeAllViews(); renderEmoji() }
            KeyboardLayer.CLIPBOARD -> bindClipboard()
            KeyboardLayer.VOICE -> bindVoice()
            KeyboardLayer.TRANSLATE -> bindTranslate()
        }
    }

    private fun bindTyping() {
        if (body.childCount != 1 || body.getChildAt(0) !== panel) {
            body.removeAllViews()
            body.addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        val spaceLabel = spaceCaption()
        val rows = KeyboardLayoutFactory.typingRows(
            mode, layer, shifted, capsLock, editorLayout, prefs.topRow, prefs.emojiPicker, enterLabel, spaceLabel, false, prefs.useEnglish, prefs.smartKeyModifiers,
            recentEmoji.ifEmpty { prefs.recentEmojis }
        )
        val rowHeight = KeyboardGeometry.rowHeightPx(prefs.keyboardSize, isLandscape(), resources.displayMetrics.density, rows.size)
        panel.bindContext(mode.name, layer.name, prefs.topRow, prefs.useEnglish, shifted, capsLock)
        panel.debug = BuildConfig.DEBUG && prefs.debugOverlay
        panel.playSpaceIntro = animateSpaceLabel
        animateSpaceLabel = false
        panel.bind(rows, rowHeight)
    }

    private fun bindRail(animated: Boolean) {
        val show = layer == KeyboardLayer.LETTERS && editorLayout == EditorLayout.TEXT
        rail.configureToolbar(prefs)
        rail.setEmptyTitle(if (show) if (prefs.useEnglish) "English" else mode.title else "")
        rail.setLanguage(prefs.useEnglish)
        rail.setClipboardVisible(showClipboardButton())
        rail.setSuggestions(if (show) candidates else emptyList(), animated && show, correctionItems)
    }

    private fun keepSuggestionRail() =
        editorLayout == EditorLayout.TEXT && layer != KeyboardLayer.TRANSLATE
    private fun spaceCaption(): String {
        val custom = prefs.customSpacebarText.trim().take(14)
        if (custom.isNotEmpty()) return custom
        return if (editorLayout != EditorLayout.TEXT) "English" else if (prefs.useEnglish) "Slashboard - English" else "Slashboard - ${mode.title}"
    }

    private fun renderNativePad() {
        val rows = when (editorLayout) {
            EditorLayout.PHONE -> listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("+","0","#"))
            EditorLayout.DATETIME -> listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("/","0",":"))
            else -> listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf(
                if (editorLayout == EditorLayout.SIGNED_NUMBER || editorLayout == EditorLayout.SIGNED_DECIMAL) "−" else "",
                "0",
                if (editorLayout == EditorLayout.DECIMAL || editorLayout == EditorLayout.SIGNED_DECIMAL) "." else ""
            ))
        }
        rows.forEachIndexed { rowIndex, values ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            values.forEachIndexed { colIndex, value ->
                if (value.isEmpty()) {
                    if (rowIndex == 3 && colIndex == 0) {
                        // Toggle to full keyboard
                        row.addView(button("ABC", utility, "Switch to normal keyboard") {
                            forceNormalKeyboard = true
                            render()
                        }, LayoutParams(0, keyHeight(), 1f).keyMargins())
                    } else {
                        row.addView(Space(context), LayoutParams(0, keyHeight(), 1f).keyMargins())
                    }
                }
                else row.addView(button(value, key, value) { actions.onCharacter(if (value == "−") "-" else value) }, LayoutParams(0, keyHeight(), 1f).keyMargins())
            }
            val action = when (rowIndex) { 0 -> backspaceButton(); 3 -> enterButton(); else -> Space(context) }
            row.addView(action, LayoutParams(0, keyHeight(), 1f).keyMargins())
            body.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, rowHeight()))
        }
        if (editorLayout == EditorLayout.PHONE) {
            val extras = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            extras.addView(button("*", utility, "Asterisk") { actions.onCharacter("*") }, LayoutParams(0, dp(46), 1f).keyMargins())
            extras.addView(button("(", utility, "Left parenthesis") { actions.onCharacter("(") }, LayoutParams(0, dp(46), 1f).keyMargins())
            extras.addView(button(")", utility, "Right parenthesis") { actions.onCharacter(")") }, LayoutParams(0, dp(46), 1f).keyMargins())
            body.addView(extras, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)))
        }
    }

    private fun renderEmoji() {
        body.clipChildren = true
        body.clipToPadding = true
        if (emojiSearch) {
            showEmojiSearch()
            return
        }
        val values = when (emojiCategoryIndex) {
            0 -> recentEmoji.ifEmpty { prefs.recentEmojis }
            else -> emojiRepo.categories.getOrNull(emojiCategoryIndex - 1)?.emoji.orEmpty()
        }
        body.addView(emojiCategoryBar(), LayoutParams(LayoutParams.MATCH_PARENT, dp(KeyboardGeometry.EMOJI_TAB_DP)))
        body.addView(emojiSectionTitle(), LayoutParams(LayoutParams.MATCH_PARENT, dp(28)))
        val typingHeight = standardContentHeight()
        
        // Calculate remaining height for the grid by subtracting tabs and bottom bar
        val gridHeight = (typingHeight - dp(KeyboardGeometry.EMOJI_TAB_DP) - dp(28) - dp(KeyboardGeometry.EMOJI_BOTTOM_DP)).coerceAtLeast(dp(120))
        val grid = if (values.isEmpty()) {
            textView("Recently used emoji appear here", 14f)
        } else {
            emojiScroller(values)
        }
        body.addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, gridHeight))
        body.addView(emojiBottomBar())
    }

    private fun emojiCategoryBar() = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        setBackgroundColor(bg)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                iconButton(org.slashboard.ime.R.drawable.ic_key_search, utility, "Search emoji") {
                    emojiSearch = true; render()
                },
                LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
            val items = listOf("Recent" to "🕒") + emojiRepo.categories.map { it.name to it.icon }
            items.forEachIndexed { i, item ->
                val selected = !emojiSearch && emojiCategoryIndex == i
                addView(emojiTab(item.second, item.first, selected) {
                    emojiSearch = false; emojiCategoryIndex = i; render()
                }, LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                    setMargins(dp(1), dp(2), dp(1), dp(2))
                })
            }
        }
        addView(row, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun emojiBottomBar() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(bg)
        addView(button("ABC", utility, "Letters") { emojiQuery = ""; emojiSearch = false; layer = KeyboardLayer.LETTERS; render() }, LayoutParams(0, dp(54), 1f).margins())
        addView(button("☺", palette.selected, "Emoji picker active") { }, LayoutParams(0, dp(54), 1f).margins())
        addView(Space(context), LayoutParams(0, dp(54), 2f))
        addView(backspaceButton(), LayoutParams(0, dp(54), 1f).margins())
    }

    private fun emojiSectionTitle() = TextView(context).apply {
        text = when (emojiCategoryIndex) {
            0 -> "Recent emoji"
            else -> emojiRepo.categories.getOrNull(emojiCategoryIndex - 1)?.name ?: "Emoji"
        }
        textSize = 12f
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setTextColor(ColorUtils.setAlphaComponent(ink, 180))
        setPadding(dp(12), 0, dp(12), 0)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun emojiTab(icon: String, description: String, selected: Boolean, click: () -> Unit) = TextView(context).apply {
        text = icon
        textSize = 18f
        gravity = Gravity.CENTER
        includeFontPadding = false
        if (Build.VERSION.SDK_INT >= 28) isFallbackLineSpacing = false
        setTextColor(ink)
        contentDescription = description
        isClickable = true
        isFocusable = true
        background = if (selected) keyBackground(utility) else android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        setOnClickListener { click() }
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        setPadding(0, dp(6), 0, dp(6))
    }
    private fun showEmojiSearch() {
        body.addView(
            textView(if (emojiQuery.isEmpty()) "Search in English or Sinhala" else emojiQuery, 15f),
            LayoutParams(LayoutParams.MATCH_PARENT, dp(34))
        )
        val sinhalaQuery = SinhalaEngine.transliterate(emojiQuery, InputMode.SMART_PHONETIC)
        val results = (emojiRepo.search(emojiQuery, 64) + emojiRepo.search(sinhalaQuery, 64)).distinct().take(80)
        val gridHeight = emojiGridHeight(2)
        when {
            emojiQuery.isEmpty() -> body.addView(textView("Type a name to find emoji", 13f), LayoutParams(LayoutParams.MATCH_PARENT, gridHeight))
            results.isEmpty() -> body.addView(textView("No emoji found", 13f), LayoutParams(LayoutParams.MATCH_PARENT, gridHeight))
            else -> body.addView(emojiScroller(results), LayoutParams(LayoutParams.MATCH_PARENT, gridHeight))
        }
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEach { keys ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            keys.forEach { c -> row.addView(button(c.toString(), key, c.toString()) { emojiQuery += c; render() }, LayoutParams(0, dp(40), 1f).margins()) }
            body.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
        }
        val bottom = LinearLayout(context).apply { orientation = HORIZONTAL }
        bottom.addView(button("ABC", utility, "Letters") { emojiQuery = ""; emojiSearch = false; layer = KeyboardLayer.LETTERS; render() }, LayoutParams(0, dp(48), 1f).margins())
        bottom.addView(button("Space", key, "Search space") { emojiQuery += " "; render() }, LayoutParams(0, dp(48), 3f).margins())
        bottom.addView(iconButton(org.slashboard.ime.R.drawable.ic_key_backspace, utility, "Delete search character") { if (emojiQuery.isNotEmpty()) emojiQuery = emojiQuery.dropLast(1); render() }, LayoutParams(0, dp(48), 1f).margins())
        body.addView(bottom)
    }
    private fun emojiScroller(values: List<String>) =
        EmojiBoard.scroller(context, values, ink, prefs.skinTone) { actions.onCharacter(it) }
    private fun showClipboardButton() =
        prefs.clipboardHistory && layer == KeyboardLayer.LETTERS && editorLayout == EditorLayout.TEXT

    private fun bindClipboard() {
        body.clipChildren = true
        body.clipToPadding = true
        val existing = body.getChildAt(0) as? ClipboardBoard
        if (existing != null && body.childCount == 1) {
            existing.configure(clipboardRecent, clipboardPinned)
            return
        }
        body.removeAllViews()
        val board = ClipboardBoard(
            context,
            KeyboardColors(key, utility, ink, palette.action, palette.actionText, palette.dark, palette.highContrast, palette.keyRadiusDp, palette.keyOpacity),
            onPaste = { clip ->
                actions.onCharacter(clip)
                layer = KeyboardLayer.LETTERS
                render()
            },
            onBack = { layer = KeyboardLayer.LETTERS; render() },
            onHide = { actions.onHide() },
            onClearRecent = {
                clipboardStore.clearHistory()
                refreshClipboardFromStore()
            },
            onPinRecent = { index ->
                clipboardStore.pin(index)
                refreshClipboardFromStore()
            },
            onRemoveRecent = { index ->
                clipboardStore.remove(index)
                refreshClipboardFromStore()
            },
            onRemovePinned = { index ->
                clipboardStore.removePinned(index)
                refreshClipboardFromStore()
            }
        )
        board.configure(clipboardRecent, clipboardPinned)
        val height = standardContentHeight()
        body.addView(board, LayoutParams(LayoutParams.MATCH_PARENT, height))
    }

    fun setVoiceManager(manager: VoiceInputManager) {
        this.voiceManager = manager
    }

    fun openVoiceTyping() {
        layer = KeyboardLayer.VOICE
        render()
    }

    private fun bindVoice() {
        val existing = body.getChildAt(0) as? VoiceTypingBoard
        if (existing != null) {
            existing.updateLanguage(prefs.useEnglish)
            return
        }
        body.removeAllViews()
        val board = VoiceTypingBoard(
            context = context,
            colors = KeyboardColors(key, utility, ink, palette.action, palette.actionText, palette.dark, palette.highContrast, palette.keyRadiusDp, palette.keyOpacity),
            actions = actions,
            useEnglish = prefs.useEnglish,
            onDismiss = {
                layer = KeyboardLayer.LETTERS
                render()
            },
            onToggleLanguage = {
                prefs.useEnglish = !prefs.useEnglish
                actions.onToolbarAction("lang_toggle")
                (body.getChildAt(0) as? VoiceTypingBoard)?.updateLanguage(prefs.useEnglish)
            }
        )
        voiceManager?.let {
            board.attachVoiceManager(it, prefs.useEnglish)
        }
        val height = standardContentHeight()
        body.addView(board, LayoutParams(LayoutParams.MATCH_PARENT, height))
        board.alpha = 0f
        board.animate().alpha(1f).setDuration(160).start()
    }

    fun openTranslator() {
        layer = KeyboardLayer.TRANSLATE
        render()
    }

    private fun bindTranslate() {
        body.removeAllViews()
        val board = TranslateBoard(
            context = context,
            colors = KeyboardColors(key, utility, ink, palette.action, palette.actionText, palette.dark, palette.highContrast, palette.keyRadiusDp, palette.keyOpacity),
            actions = actions,
            onDismiss = {
                activeTranslateBoard?.release()
                activeTranslateBoard = null
                layer = KeyboardLayer.LETTERS
                render()
            },
            onLanguageSwapped = { useEnglish ->
                prefs.useEnglish = useEnglish
                val spaceLabel = spaceCaption()
                val rows = KeyboardLayoutFactory.typingRows(
                    mode, KeyboardLayer.LETTERS, shifted, capsLock, editorLayout, prefs.topRow, prefs.emojiPicker, enterLabel, spaceLabel, false, prefs.useEnglish, prefs.smartKeyModifiers,
                    recentEmoji.ifEmpty { prefs.recentEmojis }
                )
                val rowHeight = KeyboardGeometry.rowHeightPx(prefs.keyboardSize, isLandscape(), resources.displayMetrics.density, rows.size)
                panel.bindContext(mode.name, KeyboardLayer.LETTERS.name, prefs.topRow, prefs.useEnglish, shifted, capsLock)
                panel.bind(rows, rowHeight)
            }
        )
        activeTranslateBoard = board
        body.addView(board, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Also add keyboard panel below the translate bar
        body.addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val spaceLabel = spaceCaption()
        val rows = KeyboardLayoutFactory.typingRows(
            mode, KeyboardLayer.LETTERS, shifted, capsLock, editorLayout, prefs.topRow, prefs.emojiPicker, enterLabel, spaceLabel, false, prefs.useEnglish, prefs.smartKeyModifiers,
            recentEmoji.ifEmpty { prefs.recentEmojis }
        )
        val rowHeight = KeyboardGeometry.rowHeightPx(prefs.keyboardSize, isLandscape(), resources.displayMetrics.density, rows.size)
        panel.bindContext(mode.name, KeyboardLayer.LETTERS.name, prefs.topRow, prefs.useEnglish, shifted, capsLock)
        panel.debug = BuildConfig.DEBUG && prefs.debugOverlay
        panel.playSpaceIntro = false
        panel.bind(rows, rowHeight)
    }

    private fun refreshClipboardFromStore() {
        clipboardRecent = clipboardStore.items()
        clipboardPinned = clipboardStore.pinnedItems()
        if (layer == KeyboardLayer.CLIPBOARD) render()
    }

    private fun updateShift() {
        shiftLatch.tap(android.os.SystemClock.elapsedRealtime())
        bindTyping()
    }
    private fun backspaceButton(): ImageButton {
        val b = iconButton(org.slashboard.ime.R.drawable.ic_key_backspace, utility, "Delete") { }
        var repeats = 0
        b.setOnClickListener { actions.onBackspace() }
        val repeat = object : Runnable { override fun run() { repeats++; actions.onBackspace(repeats > 20); handler.postDelayed(this, if (repeats > 20) 45 else 80) } }
        b.setOnTouchListener { _, event -> when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { repeats = 0; handler.postDelayed(repeat, 420); true }
            MotionEvent.ACTION_UP -> { handler.removeCallbacks(repeat); if (repeats == 0) b.performClick(); true }
            MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(repeat); true }
            else -> true
        } }; return b
    }
    private fun spaceButton(): Button {
        val label = spaceCaption()
        val b = button(label, key, "Space") { }
        b.textSize = KeyboardGeometry.SPACE_COLLAPSE_SP
        b.gravity = Gravity.BOTTOM or Gravity.END
        b.setPadding(dp(8), 0, dp(10), dp(7))
        b.setTextColor(ColorUtils.setAlphaComponent(ink, (255 * KeyboardGeometry.SPACE_COLLAPSE_ALPHA).toInt()))
        b.setOnClickListener { actions.onSpace() }
        var startX = 0f; var lastSteps = 0
        b.setOnTouchListener { _, e -> when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { startX = e.x; lastSteps = 0; true }
            MotionEvent.ACTION_MOVE -> { val steps = ((e.x - startX) / dp(24)).toInt(); if (steps != lastSteps) { actions.onCursorDelta(steps - lastSteps); lastSteps = steps }; true }
            MotionEvent.ACTION_UP -> { if (abs(e.x - startX) < dp(12)) b.performClick(); true }
            else -> true
        } }; return b
    }
    private fun enterButton(): View = when (enterLabel) {
        "↵" -> iconButton(org.slashboard.ime.R.drawable.ic_key_enter, utility, "Enter") { actions.onEnter() }
        "⌕" -> iconButton(org.slashboard.ime.R.drawable.ic_key_search, utility, "Enter") { actions.onEnter() }
        else -> button(enterLabel, utility, "Enter") { actions.onEnter() }
    }
    private fun button(label: String, color: Int, description: String, click: () -> Unit) = Button(context).apply {
        text = label; textSize = if (label.length > 10) 13f else 20f; isAllCaps = false; gravity = Gravity.CENTER
        setTextColor(ink); contentDescription = description; minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        background = keyBackground(color)
        stateListAnimator = null; setOnClickListener { click() }
        accessibilityDelegate = object : AccessibilityDelegate() { override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) { super.onInitializeAccessibilityNodeInfo(host, info); info.className = Button::class.java.name } }
    }
    private fun iconButton(icon: Int, color: Int, description: String, click: () -> Unit) = ImageButton(context).apply {
        setImageResource(icon); setColorFilter(ink); scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(10), dp(10), dp(10), dp(10)); contentDescription = description; background = keyBackground(color)
        stateListAnimator = null; setOnClickListener { click() }
    }
    private fun textView(value: String, size: Float) = TextView(context).apply { text = value; textSize = size; gravity = Gravity.CENTER; setTextColor(ink) }
    private fun LayoutParams.margins() = keyMargins()
    private fun LayoutParams.keyMargins() = apply {
        val horizontal = KeyboardMetrics.marginPx(prefs.keySpacing, resources.displayMetrics.density, false)
        val vertical = KeyboardMetrics.marginPx(prefs.keySpacing, resources.displayMetrics.density, true)
        setMargins(horizontal, vertical, horizontal, vertical)
    }
    private fun usesTypingPanel() = (forceNormalKeyboard || editorLayout !in numericEditors) && layer != KeyboardLayer.EMOJI && layer != KeyboardLayer.CLIPBOARD && layer != KeyboardLayer.VOICE && layer != KeyboardLayer.TRANSLATE
    private fun suggestionKeySliver() = (KeyboardGeometry.SLIVER_DP * resources.displayMetrics.density).toInt()
    private fun isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private fun inSuggestionSliver(y: Float): Boolean {
        val sliver = suggestionKeySliver()
        return y >= rail.bottom - sliver && y < rail.bottom + sliver
    }
    private fun dispatchToPanel(event: MotionEvent): Boolean {
        val transformed = MotionEvent.obtain(event)
        transformed.offsetLocation(-(body.left + panel.left).toFloat(), -(body.top + panel.top).toFloat())
        val handled = panel.dispatchTouchEvent(transformed)
        transformed.recycle()
        return handled
    }
    private fun keyBackground(base: Int): StateListDrawable {
        fun shape(color: Int) = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat(); setColor(color)
            setStroke(if (prefs.highContrast) dp(2) else 0, ink)
        }
        val pressed = ColorUtils.blendARGB(base, if (isDark()) Color.WHITE else Color.BLACK, .18f)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(pressed))
            addState(intArrayOf(), shape(base))
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun suggestionRailHeight() = KeyboardGeometry.railHeightPx(isLandscape(), resources.displayMetrics.density).toInt()
    private fun keyHeight() = if (isLandscape()) dp(42) else dp(48)
    private fun rowHeight() = if (isLandscape()) dp(48) else dp(56)

    private fun emojiGridHeight(rows: Int) = EmojiBoard.gridHeight(context, rows, isLandscape())

    private fun navigationBarFallback(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id) else 0
    }
    private fun isDark() = palette.dark

    companion object {
        val qwertyRows = listOf("qwertyuiop".map(Char::toString), "asdfghjkl".map(Char::toString), "zxcvbnm".map(Char::toString))
        val numbers = listOf("1234567890".map(Char::toString), listOf("@","#","₨","_","&","-","+","(",")","/"), listOf("*","\"","'",":",";","!","?"))
        val symbols = listOf(listOf("~","`","|","•","√","π","÷","×","¶","∆"), listOf("£","€","$","¢","^","°","=","{","}","\\"), listOf("%","©","®","™","✓","[","]"))
        val sinhalaGlyphs = listOf(
            listOf("𑇡", "𑇢", "𑇣", "𑇤", "𑇥", "𑇦", "𑇧", "𑇨", "𑇩", "𑇪"),
            listOf("𑇫", "𑇬", "𑇭", "𑇮", "𑇯", "𑇰", "𑇳", "𑇴", "෦", "෴"),
            listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")
        )
        val numericEditors = setOf(EditorLayout.NUMBER, EditorLayout.SIGNED_NUMBER, EditorLayout.DECIMAL, EditorLayout.SIGNED_DECIMAL, EditorLayout.PHONE, EditorLayout.DATETIME)
    }
}
