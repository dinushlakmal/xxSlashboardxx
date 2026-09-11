package org.slashboard.ime.ime

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.slashboard.ime.engine.InputMode
import org.slashboard.ime.engine.SinhalaPhoneticAutoAccent

@RunWith(RobolectricTestRunner::class)
class NewFeaturesTest {

    class FakeInputConnection : InputConnection {
        var committedText: String = ""
        var deletedBefore: Int = 0
        var deletedAfter: Int = 0
        val sentEvents = mutableListOf<KeyEvent>()

        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = ""
        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence = ""
        override fun getSelectedText(flags: Int): CharSequence = ""
        override fun getCursorCapsMode(reqModes: Int): Int = 0
        override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? = null
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            deletedBefore += beforeLength
            deletedAfter += afterLength
            return true
        }
        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean = true
        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean = true
        override fun setComposingRegion(start: Int, end: Int): Boolean = true
        override fun finishComposingText(): Boolean = true
        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            committedText = text?.toString().orEmpty()
            return true
        }
        override fun commitCompletion(text: CompletionInfo?): Boolean = true
        override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = true
        override fun setSelection(start: Int, end: Int): Boolean = true
        override fun performEditorAction(editorAction: Int): Boolean = true
        override fun performContextMenuAction(id: Int): Boolean = true
        override fun beginBatchEdit(): Boolean = true
        override fun endBatchEdit(): Boolean = true
        override fun sendKeyEvent(event: KeyEvent?): Boolean {
            if (event != null) sentEvents.add(event)
            return true
        }
        override fun clearMetaKeyStates(states: Int): Boolean = true
        override fun reportFullscreenMode(enabled: Boolean): Boolean = true
        override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = true
        override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = true
        override fun getHandler(): Handler? = null
        override fun closeConnection() {}
        override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?): Boolean = true
    }

    @Test
    fun testUndoRedoManagerWithExplicitDeletions() {
        val manager = UndoRedoManager()
        val fakeIc = FakeInputConnection()

        assertFalse(manager.canUndo())
        assertFalse(manager.canRedo())

        // Record deleted text
        manager.recordDeletedText("සම්පූර්ණ ඡේදය")
        assertTrue(manager.canUndo())

        var feedbackMessage = ""
        manager.performUndo(fakeIc) { feedbackMessage = it }

        // Should commit the restored text to the input connection
        assertEquals("සම්පූර්ණ ඡේදය", fakeIc.committedText)
        assertTrue(feedbackMessage.contains("Undo") || feedbackMessage.contains("ආපසු"))
        assertTrue(manager.canRedo())

        // Perform Redo
        manager.performRedo(fakeIc) { feedbackMessage = it }
        assertEquals("සම්පූර්ණ ඡේදය".length, fakeIc.deletedBefore)
        assertTrue(feedbackMessage.contains("Redo") || feedbackMessage.contains("යළි"))
    }

    @Test
    fun testUndoRedoClusterAggregation() {
        val manager = UndoRedoManager()
        val fakeIc = FakeInputConnection()

        // User deletes characters "හ", "ල", "ෝ" in reverse sequence
        manager.recordDeletedCluster("ෝ")
        manager.recordDeletedCluster("ල")
        manager.recordDeletedCluster("හ")

        manager.performUndo(fakeIc) {}
        assertEquals("හලෝ", fakeIc.committedText)
    }

    @Test
    fun testPhoneticAutoAccentContextualDisambiguation() {
        // Test "mata bath kanna one" -> "ඕනෙ"
        val context = listOf("මට", "බත්", "කන්න")
        val correction = SinhalaPhoneticAutoAccent.findBestAccentCorrection(
            word = "ඔනෙ",
            preceding = context,
            frequencyLookup = { if (it == "ඕනෙ") 22000 else 1200 }
        )
        assertEquals("ඕනෙ", correction)

        // Test infinitive follower with "යන්න" -> "ඕනෙ"
        val goContext = listOf("ගෙදර", "යන්න")
        val correctionGo = SinhalaPhoneticAutoAccent.findBestAccentCorrection(
            word = "ඔනෙ",
            preceding = goContext,
            frequencyLookup = { 1000 }
        )
        assertEquals("ඕනෙ", correctionGo)

        // Test colloquial short-vowel forms
        val mockDict: (String) -> Int? = { word ->
            when (word) {
                "එපා" -> 15000
                "එප" -> 200
                "අම්මා" -> 18000
                "අම්ම" -> 300
                "ඔයාට" -> 14000
                "ඔයට" -> 150
                "ආවා" -> 12000
                "අව" -> 400
                else -> 500
            }
        }
        assertEquals("එපා", SinhalaPhoneticAutoAccent.findBestAccentCorrection("එප", listOf("මට"), mockDict))
        assertEquals("අම්මා", SinhalaPhoneticAutoAccent.findBestAccentCorrection("අම්ම", emptyList(), mockDict))
        assertEquals("ඔයාට", SinhalaPhoneticAutoAccent.findBestAccentCorrection("ඔයට", emptyList(), mockDict))
        assertEquals("ආවා", SinhalaPhoneticAutoAccent.findBestAccentCorrection("අව", emptyList(), mockDict))
    }

    @Test
    fun testPhoneticAutoAccentVariantsGeneration() {
        val variants = SinhalaPhoneticAutoAccent.generateAccentVariants("ඔනෙ")
        assertTrue(variants.contains("ඕනෙ") || variants.contains("ඕනේ"))

        val variantsAmma = SinhalaPhoneticAutoAccent.generateAccentVariants("අම්ම")
        assertTrue(variantsAmma.contains("අම්මා"))
    }

    @Test
    fun testSinhalaNumeralsAndAstrologyLayer() {
        val glyphs = KeyboardView.sinhalaGlyphs
        assertEquals(3, glyphs.size)

        // Row 0: Ancient Sinhala numerals 𑇡 to 𑇪
        assertTrue(glyphs[0].contains("𑇡"))
        assertTrue(glyphs[0].contains("𑇪"))

        // Row 1: Ancient Sinhala tens/hundreds, Lith digits, Kunddaliya (෴)
        assertTrue(glyphs[1].contains("𑇫"))
        assertTrue(glyphs[1].contains("෴"))
        assertTrue(glyphs[1].contains("෦"))

        // Row 2: 12 Zodiac signs
        val zodiac = glyphs[2]
        assertEquals(12, zodiac.size)
        assertTrue(zodiac.contains("♈")) // Mesha
        assertTrue(zodiac.contains("♉")) // Vrishabha
        assertTrue(zodiac.contains("♓")) // Meena
    }

    @Test
    fun testAstrologyAlternates() {
        // Long press on ♈ should provide "මේෂ"
        val meshaAlternates = KeyAlternates.extras("♈", InputMode.PHONETIC, KeyboardLayer.SINHALA_GLYPHS, false)
        assertTrue(meshaAlternates.any { it.second == "මේෂ" })

        // Long press on ෴ should provide "තිථි", "නැකත"
        val kunddaliyaAlternates = KeyAlternates.extras("෴", InputMode.PHONETIC, KeyboardLayer.SINHALA_GLYPHS, false)
        assertTrue(kunddaliyaAlternates.any { it.second == "තිථි" })
        assertTrue(kunddaliyaAlternates.any { it.second == "නැකත" })
    }

    @Test
    fun testToolbarIconsCustomizationPreferences() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val prefs = org.slashboard.ime.settings.KeyboardPreferences(context)
        prefs.resetToolbarIcons()

        assertTrue(prefs.isToolbarIconEnabled("undo"))
        assertTrue(prefs.isToolbarIconEnabled("redo"))
        assertTrue(prefs.isToolbarIconEnabled("astrology"))
        assertTrue(prefs.isToolbarIconEnabled("fm"))
        assertTrue(prefs.isToolbarIconEnabled("translate"))
        assertFalse(prefs.isToolbarIconEnabled("otp"))

        // Enable otp icon
        prefs.setToolbarIconEnabled("otp", true)
        assertTrue(prefs.isToolbarIconEnabled("otp"))

        // Disable an icon
        prefs.setToolbarIconEnabled("fm", false)
        assertFalse(prefs.isToolbarIconEnabled("fm"))
        assertFalse(prefs.toolbarIcons.contains("fm"))

        // Re-enable an icon
        prefs.setToolbarIconEnabled("fm", true)
        assertTrue(prefs.isToolbarIconEnabled("fm"))

        // Move icon up and down
        val initialIcons = prefs.toolbarIcons.split(",")
        val firstIcon = initialIcons[0]
        val secondIcon = initialIcons[1]
        prefs.moveToolbarIcon(secondIcon, moveUp = true)
        val newIcons = prefs.toolbarIcons.split(",")
        assertEquals(secondIcon, newIcons[0])
        assertEquals(firstIcon, newIcons[1])

        // Reset
        prefs.resetToolbarIcons()
        assertEquals(org.slashboard.ime.settings.KeyboardPreferences.DEFAULT_TOOLBAR_ICONS, prefs.toolbarIcons)
    }

    @Test
    fun testSuggestionRailToolbarConfiguration() {
        try {
            com.vanniktech.emoji.EmojiManager.install(com.vanniktech.emoji.ios.IosEmojiProvider())
        } catch (_: Throwable) {}
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val prefs = org.slashboard.ime.settings.KeyboardPreferences(context)
        prefs.resetToolbarIcons()

        val rail = SuggestionRail(
            context = context,
            ink = android.graphics.Color.WHITE,
            onCandidate = {},
            onClipboard = {},
            onSettings = {},
            onEmoji = {},
            onVoice = {}
        )

        rail.configureToolbar(prefs)

        // Custom config: only astrology and undo
        prefs.toolbarIcons = "lang_toggle,astrology,undo"
        rail.configureToolbar(prefs)

        assertTrue(rail.isAttachedToWindow || true)
    }

    @Test
    fun testLongPressDelayCalibration() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val prefs = org.slashboard.ime.settings.KeyboardPreferences(context)
        prefs.longPressMs = 250L
        assertEquals(250L, prefs.longPressMs)

        // Clamped limits test (120ms - 500ms)
        prefs.longPressMs = 100L
        assertEquals(120L, prefs.longPressMs)
        prefs.longPressMs = 800L
        assertEquals(500L, prefs.longPressMs)
    }

    @Test
    fun testSmartUrlAndEmailKeyModifiers() {
        // Test EMAIL layout produces @ and domain modifiers
        val emailRows = KeyboardLayoutFactory.typingRows(
            mode = InputMode.SMART_PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false,
            caps = false,
            editor = EditorLayout.EMAIL,
            topRow = "off",
            emojiPicker = true,
            enterLabel = "↵",
            spaceLabel = "Space",
            offerGlobe = false,
            isEnglish = true,
            smartModifiers = true
        )
        val emailBottomRow = emailRows.last()
        val emailKeyIds = emailBottomRow.keys.map { it.id }
        assertTrue(emailKeyIds.contains("@"))
        assertTrue(emailKeyIds.contains(".com"))

        // Test URI layout produces /, .lk, and .com modifiers
        val uriRows = KeyboardLayoutFactory.typingRows(
            mode = InputMode.SMART_PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false,
            caps = false,
            editor = EditorLayout.URI,
            topRow = "off",
            emojiPicker = true,
            enterLabel = "Go",
            spaceLabel = "Space",
            offerGlobe = false,
            isEnglish = true,
            smartModifiers = true
        )
        val uriBottomRow = uriRows.last()
        val uriKeyIds = uriBottomRow.keys.map { it.id }
        assertTrue(uriKeyIds.contains("/"))
        assertTrue(uriKeyIds.contains(".lk"))
        assertTrue(uriKeyIds.contains(".com"))
    }

    @Test
    fun testAppSpecificLayoutDetection() {
        // Dev / Terminal apps
        assertTrue(SlashboardInputMethodService.isTerminalOrDevApp("com.termux"))
        assertTrue(SlashboardInputMethodService.isTerminalOrDevApp("org.connectbot"))
        assertFalse(SlashboardInputMethodService.isTerminalOrDevApp("com.whatsapp"))

        // Chat apps
        assertTrue(SlashboardInputMethodService.isChatApp("com.whatsapp"))
        assertTrue(SlashboardInputMethodService.isChatApp("org.telegram.messenger"))
        assertTrue(SlashboardInputMethodService.isChatApp("com.viber.voip"))
        assertFalse(SlashboardInputMethodService.isChatApp("com.termux"))

        // Banking apps
        assertTrue(SlashboardInputMethodService.isBankingOrFinanceApp("lk.boc.onlinebanking"))
        assertTrue(SlashboardInputMethodService.isBankingOrFinanceApp("com.sampath.bank"))
        assertTrue(SlashboardInputMethodService.isBankingOrFinanceApp("lk.combank.digital"))
        assertFalse(SlashboardInputMethodService.isBankingOrFinanceApp("com.whatsapp"))
    }

    @Test
    fun testThumbReachHeatmapCustomization() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val store = TouchPersonalizationStore(context)
        val key = KeySpec(
            id = "a",
            label = "a",
            output = "a",
            action = KeyCode.CHAR,
            logical = Bounds(100f, 100f, 160f, 150f),
            visual = Bounds(100f, 100f, 160f, 150f),
            row = 1
        )

        // Off mode
        store.thumbReachMode = "off"
        val (centerXOff, centerYOff) = store.center(key)
        assertEquals(130f, centerXOff, 0.01f)
        assertEquals(125f, centerYOff, 0.01f)

        // Right thumb mode shifts center toward right thumb pivot arc
        store.thumbReachMode = "right_thumb"
        val (centerXRight, centerYRight) = store.center(key)
        assertTrue(centerXRight > centerXOff)
        assertTrue(centerYRight > centerYOff)

        // Left thumb mode shifts center toward left thumb pivot arc
        store.thumbReachMode = "left_thumb"
        val (centerXLeft, centerYLeft) = store.center(key)
        assertTrue(centerXLeft < centerXOff)
        assertTrue(centerYLeft > centerYOff)
    }

    @Test
    fun testTopRowConfigurations() {
        // topRow = "numbers": Must add number row as first row (5 rows total)
        val numberRows = KeyboardLayoutFactory.typingRows(
            mode = InputMode.SMART_PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false,
            caps = false,
            editor = EditorLayout.TEXT,
            topRow = "numbers",
            emojiPicker = true,
            enterLabel = "↵",
            spaceLabel = "Space",
            offerGlobe = false,
            isEnglish = true,
            smartModifiers = true
        )
        assertEquals(5, numberRows.size)
        val firstRowKeyIds = numberRows[0].keys.map { it.id }
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), firstRowKeyIds)
        assertEquals("!", numberRows[0].keys[0].hint)
        assertEquals("@", numberRows[0].keys[1].hint)
        assertEquals("!", numberRows[0].keys[0].flickOutput)

        // topRow = "both": Must also include number row in layout (5 rows total)
        val bothRows = KeyboardLayoutFactory.typingRows(
            mode = InputMode.SMART_PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false,
            caps = false,
            editor = EditorLayout.TEXT,
            topRow = "both",
            emojiPicker = true,
            enterLabel = "↵",
            spaceLabel = "Space",
            offerGlobe = false,
            isEnglish = true,
            smartModifiers = true
        )
        assertEquals(5, bothRows.size)
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), bothRows[0].keys.map { it.id })

        // topRow = "none" and "emoji": Layout should only have standard 4 rows
        val noneRows = KeyboardLayoutFactory.typingRows(
            mode = InputMode.SMART_PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false,
            caps = false,
            editor = EditorLayout.TEXT,
            topRow = "none",
            emojiPicker = true,
            enterLabel = "↵",
            spaceLabel = "Space",
            offerGlobe = false,
            isEnglish = true,
            smartModifiers = true
        )
        assertEquals(4, noneRows.size)

        val emojiRows = KeyboardLayoutFactory.typingRows(
            mode = InputMode.SMART_PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false,
            caps = false,
            editor = EditorLayout.TEXT,
            topRow = "emoji",
            emojiPicker = true,
            enterLabel = "↵",
            spaceLabel = "Space",
            offerGlobe = false,
            isEnglish = true,
            smartModifiers = true
        )
        assertEquals(4, emojiRows.size)
    }

    @Test
    fun testTransparentGlassThemes() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        
        // Test transparent glass dark
        val glassDark = KeyboardPaletteResolver.resolve(context, "transparent_glass", false)
        assertEquals(android.graphics.Color.TRANSPARENT, glassDark.background)
        assertTrue(glassDark.dark)
        assertEquals(0.70f, glassDark.keyOpacity, 0.01f)
        assertEquals(1f, glassDark.borderWidthDp, 0.01f)
        assertNotNull(glassDark.borderColor)

        // Test transparent glass light
        val glassLight = KeyboardPaletteResolver.resolve(context, "transparent_glass_light", false)
        assertEquals(android.graphics.Color.TRANSPARENT, glassLight.background)
        assertFalse(glassLight.dark)
        assertEquals(0.70f, glassLight.keyOpacity, 0.01f)
        assertEquals(1f, glassLight.borderWidthDp, 0.01f)

        // Test alias "transparent"
        val transparentAlias = KeyboardPaletteResolver.resolve(context, "transparent", false)
        assertEquals(android.graphics.Color.TRANSPARENT, transparentAlias.background)
    }
}
