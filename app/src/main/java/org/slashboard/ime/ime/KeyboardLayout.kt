package org.slashboard.ime.ime

import org.slashboard.ime.engine.InputMode
import org.slashboard.ime.engine.SinhalaEngine

internal data class KeyboardLayout(
    val keys: List<KeySpec>,
    val width: Float,
    val height: Float,
    val rowHeight: Float,
    val letterWidth: Float,
    val rows: Int
) {
    fun keyAtLogical(x: Float, y: Float): KeySpec? = keys.firstOrNull { it.logical.contains(x, y) }
    fun keyById(id: String): KeySpec? = keys.firstOrNull { it.id == id }
    fun rowKeys(row: Int): List<KeySpec> = keys.filter { it.row == row }
}

internal object KeyboardLayoutFactory {
    fun place(
        rows: List<RowDef>,
        width: Float,
        rowHeight: Float,
        insetH: Float,
        insetV: Float,
        sliver: Float
    ): KeyboardLayout {
        val specs = ArrayList<KeySpec>(48)
        var currentY = 0f
        rows.forEachIndexed { rowIndex, row ->
            val currRowHeight = rowHeight * row.heightFactor
            val y = currentY
            var x = row.startFraction * width
            row.keys.forEachIndexed { index, def ->
                val cell = def.widthFraction * width
                val visualLeft = x
                val visualRight = x + cell
                var logicalLeft = visualLeft
                var logicalRight = visualRight
                if (row.expandEdges && index == 0) logicalLeft = 0f
                if (row.expandEdges && index == row.keys.lastIndex) logicalRight = width
                val topSliver = if (row.sliverTop) sliver else 0f
                val logical = Bounds(logicalLeft, y - topSliver, logicalRight, y + currRowHeight)
                val letter = width * KeyboardGeometry.LETTER
                val scale = if (letter > 0f) (cell / letter).coerceIn(0.55f, 1f) else 1f
                val ih = (insetH * scale).coerceAtMost(cell * 0.22f)
                val iv = (insetV * scale.coerceAtLeast(0.75f) * row.heightFactor.coerceAtLeast(0.55f)).coerceAtMost(currRowHeight * 0.18f)
                val visual = Bounds(
                    visualLeft + ih,
                    y + iv,
                    visualRight - ih,
                    y + currRowHeight - iv
                )
                specs += KeySpec(
                    id = def.id,
                    label = def.label,
                    output = def.output,
                    action = def.action,
                    logical = logical,
                    visual = visual,
                    row = rowIndex,
                    hint = def.hint,
                    extras = def.extras,
                    flickOutput = def.flickOutput,
                    icon = def.icon,
                    utility = def.utility,
                    payload = def.payload
                )
                x += cell
            }
            currentY += currRowHeight
        }
        val letterWidth = specs.filter { !it.utility && it.action == KeyCode.CHAR }
            .map { it.logical.width }
            .average()
            .toFloat()
            .takeIf { it.isFinite() && it > 0f }
            ?: width * KeyboardGeometry.LETTER
        stealSpaceHits(specs)
        return KeyboardLayout(specs, width, currentY, rowHeight, letterWidth, rows.size)
    }

    /** Space is used far more than "." / emoji; give it the gutter and a sliver of each neighbour. */
    private fun stealSpaceHits(specs: ArrayList<KeySpec>, fraction: Float = KeyboardGeometry.SPACE_STEAL) {
        val spaces = specs.indices.filter { specs[it].action == KeyCode.SPACE }
        for (spaceIndex in spaces) {
            val space = specs[spaceIndex]
            val row = specs.mapIndexed { index, key -> index to key }
                .filter { it.second.row == space.row }
                .sortedBy { it.second.logical.left }
            val position = row.indexOfFirst { it.first == spaceIndex }
            if (position < 0) continue
            if (position > 0) stealFromNeighbor(specs, spaceIndex, row[position - 1].first, fromRight = true, fraction)
            if (position < row.lastIndex) stealFromNeighbor(specs, spaceIndex, row[position + 1].first, fromRight = false, fraction)
        }
    }

    private fun stealFromNeighbor(
        specs: ArrayList<KeySpec>,
        spaceIndex: Int,
        neighborIndex: Int,
        fromRight: Boolean,
        fraction: Float
    ) {
        val space = specs[spaceIndex]
        val neighbor = specs[neighborIndex]
        val amount = neighbor.logical.width * fraction
        if (amount <= 0f) return
        if (fromRight) {
            specs[neighborIndex] = neighbor.copy(logical = neighbor.logical.copy(right = neighbor.logical.right - amount))
            specs[spaceIndex] = space.copy(logical = space.logical.copy(left = space.logical.left - amount))
        } else {
            specs[neighborIndex] = neighbor.copy(logical = neighbor.logical.copy(left = neighbor.logical.left + amount))
            specs[spaceIndex] = space.copy(logical = space.logical.copy(right = space.logical.right + amount))
        }
    }

        fun typingRows(
        mode: InputMode,
        layer: KeyboardLayer,
        shifted: Boolean,
        caps: Boolean,
        editor: EditorLayout,
        topRow: String,
        emojiPicker: Boolean,
        enterLabel: String,
        spaceLabel: String,
        offerGlobe: Boolean = false,
        isEnglish: Boolean = false,
        smartModifiers: Boolean = true,
        recentEmojis: List<String> = emptyList()
    ): List<RowDef> = when (layer) {
        KeyboardLayer.LETTERS -> letterRows(mode, shifted, caps, editor, topRow, emojiPicker, enterLabel, spaceLabel, offerGlobe, isEnglish, smartModifiers, recentEmojis)
        KeyboardLayer.NUMBERS -> symbolRows(KeyboardView.numbers, KeyboardLayer.SYMBOLS, "=\\<", enterLabel, spaceLabel, emojiPicker)
        KeyboardLayer.SYMBOLS -> symbolRows(KeyboardView.symbols, KeyboardLayer.SINHALA_GLYPHS, "𑇡♈", enterLabel, spaceLabel, emojiPicker)
        KeyboardLayer.SINHALA_GLYPHS -> symbolRows(KeyboardView.sinhalaGlyphs, KeyboardLayer.NUMBERS, "?123", enterLabel, spaceLabel, emojiPicker)
        else -> emptyList()
    }

    private fun letterRows(
        mode: InputMode,
        shifted: Boolean,
        caps: Boolean,
        editor: EditorLayout,
        topRow: String,
        emojiPicker: Boolean,
        enterLabel: String,
        spaceLabel: String,
        offerGlobe: Boolean,
        isEnglish: Boolean,
        smartModifiers: Boolean = true,
        recentEmojis: List<String> = emptyList()
    ): List<RowDef> {
        val rows = ArrayList<RowDef>(6)
        val showTopRow = editor == EditorLayout.TEXT
        if (showTopRow && (topRow == "emoji" || topRow == "both")) {
            val defaultEmojis = listOf("😀", "😂", "❤️", "👍", "🙏", "🔥", "✨", "🎉", "🇱🇰", "😊")
            val emojisToShow = if (recentEmojis.isNotEmpty()) {
                (recentEmojis + defaultEmojis).distinct().take(10)
            } else {
                defaultEmojis
            }
            rows += RowDef(
                emojisToShow.map { charDef(it, it) },
                expandEdges = true,
                sliverTop = true,
                heightFactor = 0.62f
            )
        }
        if (showTopRow && (topRow == "numbers" || topRow == "both")) {
            rows += RowDef(
                "1234567890".map { charDef(it.toString(), it.toString()) },
                expandEdges = true,
                sliverTop = topRow == "numbers",
                heightFactor = 0.68f
            )
        }
        val firstLetters = rows.isEmpty()
        val isWijesekara = false
        val q = KeyboardView.qwertyRows[0].map { letterDef(it, mode, isWijesekara, shifted, caps, KeyboardGeometry.LETTER, isEnglish) }
        val a = KeyboardView.qwertyRows[1].map { letterDef(it, mode, isWijesekara, shifted, caps, KeyboardGeometry.LETTER, isEnglish) }
        val z = KeyboardView.qwertyRows[2].map { letterDef(it, mode, isWijesekara, shifted, caps, KeyboardGeometry.LETTER, isEnglish) }
        rows += RowDef(q, startFraction = 0f, expandEdges = true, sliverTop = firstLetters)
        rows += RowDef(a, startFraction = KeyboardGeometry.ROW2_OFFSET, expandEdges = true)
        rows += RowDef(
            listOf(shiftDef(shifted, caps).copy(widthFraction = KeyboardGeometry.SHIFT)) +
                z +
                listOf(deleteDef().copy(widthFraction = KeyboardGeometry.DELETE))
        )
        rows += bottomRow(editor, emojiPicker, enterLabel, spaceLabel, offerGlobe, smartModifiers)
        return rows
    }

    private fun wijesekaraThirdFractions(letterCount: Int): List<Float> {
        val shift = 0.12f
        val rest = 1f - shift * 2
        return listOf(shift) + List(letterCount) { rest / letterCount } + listOf(shift)
    }

    private fun symbolRows(
        grid: List<List<String>>,
        alternate: KeyboardLayer,
        alternateLabel: String,
        enterLabel: String,
        spaceLabel: String,
        emojiPicker: Boolean
    ): List<RowDef> {
        val rows = grid.take(2).mapIndexed { index, labels ->
            val fraction = 1f / labels.size
            RowDef(labels.map { charDef(it, it, fraction) }, expandEdges = true, sliverTop = index == 0)
        }.toMutableList()

        val third = grid.getOrElse(2) { emptyList() }
        val characterWidth = (1f - KeyboardGeometry.SYMBOLS - KeyboardGeometry.DELETE) / third.size.coerceAtLeast(1)
        rows += RowDef(
            listOf(
                KeyDef(alternateLabel, alternateLabel, "", KeyCode.LAYER, KeyboardGeometry.SYMBOLS, utility = true, payload = alternate.name),
            ) + third.map { charDef(it, it, characterWidth) } +
                deleteDef().copy(widthFraction = KeyboardGeometry.DELETE)
        )

        val bottom = ArrayList<KeyDef>(5)
        bottom += KeyDef("ABC", "ABC", "", KeyCode.LAYER, KeyboardGeometry.SYMBOLS, utility = true, payload = KeyboardLayer.LETTERS.name)
        if (emojiPicker) {
            bottom += KeyDef("emoji", "", "", KeyCode.EMOJI, KeyboardGeometry.PUNCT, icon = org.slashboard.ime.R.drawable.ic_key_emoji, utility = true)
        }
        bottom += spaceDef(1f - bottom.sumOf { it.widthFraction.toDouble() }.toFloat() - KeyboardGeometry.PERIOD - KeyboardGeometry.ENTER, spaceLabel)
        bottom += periodDef()
        bottom += enterDef(enterLabel)
        rows += RowDef(bottom, expandEdges = true)
        return rows
    }

    private fun bottomRow(
        editor: EditorLayout,
        emojiPicker: Boolean,
        enterLabel: String,
        spaceLabel: String,
        offerGlobe: Boolean,
        smartModifiers: Boolean = true
    ): RowDef {
        val keys = ArrayList<KeyDef>(10)
        keys += KeyDef("?123", "?123", "", KeyCode.LAYER, KeyboardGeometry.SYMBOLS, utility = true, payload = KeyboardLayer.NUMBERS.name)
        
        if (smartModifiers && editor == EditorLayout.EMAIL) {
            // Smart Email keys
            keys += charDef("@", "@", 0.075f)
            keys += charDef(".", ".", 0.065f)
        } else if (smartModifiers && editor == EditorLayout.URI) {
            // Smart URL keys
            keys += charDef("/", "/", 0.075f)
            keys += charDef(".", ".", 0.065f)
        } else {
            when (editor) {
                EditorLayout.EMAIL -> keys += charDef("@", "@", KeyboardGeometry.PUNCT)
                EditorLayout.URI -> keys += charDef("/", "/", KeyboardGeometry.PUNCT)
                else -> Unit
            }
        }
        
        if (editor == EditorLayout.TEXT) {
            keys += charDef(",", ",", KeyboardGeometry.COMMA)
            keys += KeyDef("globe", "", "", KeyCode.GLOBE, KeyboardGeometry.GLOBE, icon = org.slashboard.ime.R.drawable.ic_key_globe, utility = true)
        }
        
        val trailing = ArrayList<KeyDef>(4)
        if (smartModifiers && editor == EditorLayout.URI) {
            // Quick domain shortcut in URL bar: .lk and .com
            trailing += KeyDef(".lk", ".lk", ".lk", KeyCode.CHAR, 0.10f)
            trailing += KeyDef(".com", ".com", ".com", KeyCode.CHAR, 0.11f)
        } else if (smartModifiers && editor == EditorLayout.EMAIL) {
            // Quick domain shortcut in Email field: .com
            trailing += KeyDef(".com", ".com", ".com", KeyCode.CHAR, 0.12f)
        } else {
            if (editor == EditorLayout.TEXT || editor == EditorLayout.EMAIL || editor == EditorLayout.URI) {
                trailing += periodDef()
            }
        }
        trailing += enterDef(enterLabel)
        
        val used = keys.sumOf { it.widthFraction.toDouble() } + trailing.sumOf { it.widthFraction.toDouble() }
        keys += spaceDef((1.0 - used).toFloat().coerceAtLeast(0.20f), spaceLabel)
        keys += trailing
        return RowDef(keys, expandEdges = true)
    }

    private fun letterDef(
        id: String,
        mode: InputMode,
        wijesekara: Boolean,
        shifted: Boolean,
        caps: Boolean,
        width: Float = KeyboardGeometry.LETTER,
        isEnglish: Boolean = false
    ): KeyDef {
        val label = letterLabel(id, wijesekara, shifted, caps)
        val output = if (wijesekara) SinhalaEngine.slsCharacter(id, shifted || caps) else label
        val extras = KeyAlternates.extras(id, mode, KeyboardLayer.LETTERS, shifted || caps)
        val numberHint = when (id) {
            "q" -> "1"; "w" -> "2"; "e" -> "3"; "r" -> "4"; "t" -> "5"
            "y" -> "6"; "u" -> "7"; "i" -> "8"; "o" -> "9"; "p" -> "0"
            else -> null
        }
        val hint = if (isEnglish) numberHint else (KeyAlternates.hint(id, mode, KeyboardLayer.LETTERS) ?: phoneticHint(id, mode, wijesekara, shifted, caps) ?: numberHint)
        val fullExtras = if (isEnglish && numberHint != null && extras.none { it.second == numberHint }) {
            listOf(numberHint to numberHint) + extras
        } else {
            extras
        }
        val flick = fullExtras.firstOrNull()?.second
        return KeyDef(id, label, output, KeyCode.CHAR, width, hint, fullExtras, flick)
    }

    private fun charDef(id: String, output: String, width: Float = KeyboardGeometry.LETTER): KeyDef {
        val extras = KeyAlternates.extras(id, InputMode.PHONETIC, KeyboardLayer.NUMBERS, false)
        return KeyDef(id, id, output, KeyCode.CHAR, width, extras = extras, flickOutput = extras.firstOrNull()?.second)
    }

    private fun periodDef(): KeyDef {
        val extras = KeyAlternates.extras(".", InputMode.PHONETIC, KeyboardLayer.NUMBERS, false)
        return KeyDef(
            ".", ".", ".", KeyCode.CHAR, KeyboardGeometry.PUNCT,
            hint = ",", extras = extras, flickOutput = extras.firstOrNull()?.second
        )
    }

    private fun shiftDef(shifted: Boolean, caps: Boolean) = KeyDef(
        KeyRow.SHIFT, "", "", KeyCode.SHIFT, KeyboardGeometry.SHIFT,
        icon = if (caps || shifted) org.slashboard.ime.R.drawable.ic_key_caps else org.slashboard.ime.R.drawable.ic_key_shift,
        utility = true
    )

    private fun deleteDef() = KeyDef(
        KeyRow.DELETE, "", "", KeyCode.DELETE, KeyboardGeometry.DELETE,
        icon = org.slashboard.ime.R.drawable.ic_key_backspace, utility = true
    )

    private fun spaceDef(width: Float, label: String) = KeyDef("space", label, " ", KeyCode.SPACE, width)

    private fun enterDef(enterLabel: String) = KeyDef(
        "enter",
        if (enterLabel == "↵" || enterLabel == "⌕") "" else enterLabel,
        "",
        KeyCode.ENTER,
        KeyboardGeometry.ENTER,
        icon = when (enterLabel) {
            "↵" -> org.slashboard.ime.R.drawable.ic_key_enter
            "⌕" -> org.slashboard.ime.R.drawable.ic_key_search
            else -> null
        },
        utility = true
    )

    fun letterLabel(id: String, wijesekara: Boolean, shifted: Boolean, caps: Boolean) = when {
        id == "rakaranshaya" -> if (shifted || caps) "ZWJ" else "්‍ර"
        id == "h" && wijesekara && (shifted || caps) -> "්‍ය"
        wijesekara -> SinhalaEngine.slsKeyLabel(id.single(), shifted || caps)
        shifted || caps -> id.uppercase()
        else -> id
    }

    fun phoneticHint(id: String, mode: InputMode, wijesekara: Boolean, shifted: Boolean, caps: Boolean): String? {
        if (wijesekara || id.length != 1) return null
        val source = if (shifted || caps) id.uppercase() else id
        val rendered = SinhalaEngine.transliterate(source, mode)
        val hint = StringBuilder()
        rendered.codePoints().forEach { cp -> if (cp in 0x0D80..0x0DFF && cp != 0x0DCA) hint.appendCodePoint(cp) }
        return hint.toString().takeIf { it.isNotEmpty() }
    }
}
