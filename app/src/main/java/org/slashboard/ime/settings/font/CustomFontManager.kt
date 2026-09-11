package org.slashboard.ime.settings.font

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class FontItem(
    val id: String,
    val name: String,
    val category: String,
    val sample: String = "Slashboard Keyboard 2026",
    val isCustomFile: Boolean = false,
    val filePath: String? = null,
    val fontFamilyType: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val transformer: (String) -> String = { it }
)

object CustomFontManager {

    val CATEGORIES = listOf(
        "All",
        "Sans Serif",
        "Serif",
        "Monospace",
        "Handwriting",
        "Display",
        "Decorative",
        "Cursive",
        "Pixel",
        "Retro",
        "Modern",
        "Minimal",
        "Bold",
        "Light",
        "Condensed",
        "Extended",
        "Outline",
        "Shadow",
        "Graffiti",
        "Gothic",
        "Calligraphy",
        "Comic",
        "Tech",
        "Futuristic",
        "Vintage",
        "Art Deco",
        "Brush",
        "Stencil",
        "Neon",
        "Sinhala",
        "Custom Imported"
    )

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, "fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getImportedFonts(context: Context): List<FontItem> {
        val dir = getFontsDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".ttf") || name.endsWith(".otf") } ?: return emptyList()
        return files.map { file ->
            FontItem(
                id = "custom_file_" + file.nameWithoutExtension,
                name = file.nameWithoutExtension.replace("_", " ").capitalizeWords(),
                category = "Custom Imported",
                isCustomFile = true,
                filePath = file.absolutePath
            )
        }
    }

    fun importFont(context: Context, uri: Uri): FontItem? {
        return try {
            val contentResolver = context.contentResolver
            val name = getFileName(context, uri) ?: ("custom_font_" + System.currentTimeMillis() + ".ttf")
            val cleanName = name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val destFile = File(getFontsDir(context), cleanName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            FontItem(
                id = "custom_file_" + destFile.nameWithoutExtension,
                name = destFile.nameWithoutExtension.replace("_", " ").capitalizeWords(),
                category = "Custom Imported",
                isCustomFile = true,
                filePath = destFile.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) it.substring(cut + 1) else it
            }
        }
        return result
    }

    fun getTypeface(context: Context, fontId: String): Typeface {
        if (fontId.startsWith("custom_file_")) {
            val dir = getFontsDir(context)
            val name = fontId.removePrefix("custom_file_")
            val ttf = File(dir, "$name.ttf")
            val otf = File(dir, "$name.otf")
            val file = if (ttf.exists()) ttf else if (otf.exists()) otf else null
            if (file != null) {
                try {
                    return Typeface.createFromFile(file)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val item = getFontItem(context, fontId)
        val familyType = item?.fontFamilyType
        val isBold = item?.isBold == true || fontId.contains("bold") || item?.category == "Bold"
        val isItalic = item?.isItalic == true || fontId.contains("italic") || item?.category in listOf("Cursive", "Handwriting", "Calligraphy")

        val baseFamily = when {
            familyType == "serif" || fontId.contains("serif") || item?.category == "Serif" -> Typeface.SERIF
            familyType == "monospace" || fontId.contains("mono") || fontId.contains("code") || item?.category == "Monospace" -> Typeface.MONOSPACE
            familyType == "cursive" || item?.category in listOf("Cursive", "Handwriting", "Calligraphy") -> {
                try {
                    Typeface.create("cursive", Typeface.NORMAL) ?: Typeface.SERIF
                } catch (e: Exception) {
                    Typeface.SERIF
                }
            }
            familyType == "sans_serif" -> Typeface.SANS_SERIF
            else -> Typeface.DEFAULT
        }

        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        return try {
            Typeface.create(baseFamily, style)
        } catch (e: Exception) {
            baseFamily
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }

    // Generates the comprehensive 500+ font styles across 30 categories
    fun getAllFonts(context: Context): List<FontItem> {
        val list = mutableListOf<FontItem>()

        // 1. System Default (MANDATORY at position 0)
        list.add(FontItem("default", "System Default", "Sans Serif", sample = "Default Font", transformer = { it }))
        list.add(FontItem("sans_serif", "Roboto Clean", "Sans Serif", sample = "Roboto Clean", fontFamilyType = "sans_serif", transformer = { it }))
        list.add(FontItem("serif", "Classic Serif", "Serif", sample = "Classic Serif", fontFamilyType = "serif", transformer = { toSerif(it) }))
        list.add(FontItem("monospace", "Source Code Pro", "Monospace", sample = "Source Code", fontFamilyType = "monospace", transformer = { toMonospace(it) }))
        list.add(FontItem("bold", "Impact Bold", "Bold", sample = "Impact Bold", isBold = true, transformer = { toSansBold(it) }))

        // 2. Add Imported Fonts
        list.addAll(getImportedFonts(context))

        // 3. Generate 500+ Curated Unicode Styled Fonts & Transformations
        buildUnicodeFontCatalog(list)

        return list
    }

    fun getFontItem(context: Context, fontId: String): FontItem? {
        return getAllFonts(context).firstOrNull { it.id == fontId }
    }

    fun transformText(context: Context, fontId: String, text: String): String {
        if (fontId == "default" || fontId.isEmpty()) return text
        val font = getFontItem(context, fontId) ?: return text
        return font.transformer(text)
    }

    private fun buildUnicodeFontCatalog(list: MutableList<FontItem>) {
        // --- 1. Sans Serif ---
        list.add(FontItem("sans_bold", "Sans Bold", "Sans Serif", fontFamilyType = "sans_serif", isBold = true) { toSansBold(it) })
        list.add(FontItem("sans_italic", "Sans Italic", "Sans Serif", fontFamilyType = "sans_serif", isItalic = true) { toSansItalic(it) })
        list.add(FontItem("sans_bold_italic", "Sans Bold Italic", "Sans Serif", fontFamilyType = "sans_serif", isBold = true, isItalic = true) { toSansBoldItalic(it) })
        list.add(FontItem("sans_spaced", "Sans Wide Spaced", "Sans Serif", fontFamilyType = "sans_serif") { it.map { c -> "$c " }.joinToString("").trim() })
        list.add(FontItem("sans_clean", "Modern Sans Clean", "Sans Serif", fontFamilyType = "sans_serif") { toSans(it) })

        // --- 2. Serif ---
        list.add(FontItem("serif_bold", "Serif Bold", "Serif", fontFamilyType = "serif", isBold = true) { toSerifBold(it) })
        list.add(FontItem("serif_italic", "Serif Italic", "Serif", fontFamilyType = "serif", isItalic = true) { toSerifItalic(it) })
        list.add(FontItem("serif_bold_italic", "Serif Bold Italic", "Serif", fontFamilyType = "serif", isBold = true, isItalic = true) { toSerifBoldItalic(it) })
        list.add(FontItem("serif_royal", "Royal Serif", "Serif", fontFamilyType = "serif", isBold = true) { "👑 " + toSerifBold(it) + " 👑" })
        list.add(FontItem("serif_editorial", "Editorial Serif", "Serif", fontFamilyType = "serif") { toSerif(it) })

        // --- 3. Monospace ---
        list.add(FontItem("monospace_clean", "Monospace Coding", "Monospace", fontFamilyType = "monospace") { toMonospace(it) })
        list.add(FontItem("monospace_terminal", "Terminal Green", "Monospace", fontFamilyType = "monospace") { "> " + toMonospace(it) })
        list.add(FontItem("monospace_bracket", "Bracketed Code", "Monospace", fontFamilyType = "monospace") { "[$it]" })
        list.add(FontItem("monospace_box", "ASCII Code Box", "Monospace", fontFamilyType = "monospace") { "/* $it */" })
        list.add(FontItem("monospace_slash", "Slash Code", "Monospace", fontFamilyType = "monospace") { "// " + toMonospace(it) })

        // --- 4. Handwriting ---
        list.add(FontItem("hand_script", "Smooth Script", "Handwriting", fontFamilyType = "cursive", isItalic = true) { toScript(it) })
        list.add(FontItem("hand_bold_script", "Bold Handwriting", "Handwriting", fontFamilyType = "cursive", isBold = true, isItalic = true) { toBoldScript(it) })
        list.add(FontItem("hand_signature", "Signature Ink", "Handwriting", fontFamilyType = "cursive", isItalic = true) { "✍️ " + toScript(it) })
        list.add(FontItem("hand_journal", "Journal Diary", "Handwriting", fontFamilyType = "cursive", isBold = true) { "📖 " + toBoldScript(it) })
        list.add(FontItem("hand_freestyle", "Freestyle Cursive", "Handwriting", fontFamilyType = "cursive", isItalic = true) { toScript(it) + " ✨" })

        // --- 5. Display ---
        list.add(FontItem("display_double", "Double Struck / Blackboard", "Display", isBold = true) { toDoubleStruck(it) })
        list.add(FontItem("display_smallcaps", "Grand Small Caps", "Display") { toSmallCaps(it) })
        list.add(FontItem("display_circled", "Circled White", "Display") { toCircled(it) })
        list.add(FontItem("display_dark_circle", "Circled Inverted Black", "Display") { toCircledDark(it) })
        list.add(FontItem("display_boxed", "Boxed Square", "Display") { toSquared(it) })
        list.add(FontItem("display_boxed_dark", "Boxed Inverted Black", "Display") { toSquaredDark(it) })

        // --- 6. Decorative ---
        list.add(FontItem("dec_sparkle", "Glitter & Sparkles", "Decorative") { "✨ $it ✨" })
        list.add(FontItem("dec_hearts", "Love Heart Border", "Decorative") { "❤️ $it ❤️" })
        list.add(FontItem("dec_stars", "Starry Night", "Decorative") { "★ $it ★" })
        list.add(FontItem("dec_music", "Musical Symphony", "Decorative") { "🎵 $it 🎶" })
        list.add(FontItem("dec_crown", "Majestic Crown", "Decorative", isBold = true) { "👑 $it 👑" })
        list.add(FontItem("dec_butterfly", "Butterfly Flutter", "Decorative") { "🦋 $it 🦋" })
        list.add(FontItem("dec_fire", "Blazing Fire", "Decorative", isBold = true) { "🔥 $it 🔥" })
        list.add(FontItem("dec_flowers", "Blossom Flower", "Decorative") { "🌸 $it 🌸" })
        list.add(FontItem("dec_diamond", "Crystal Diamond", "Decorative") { "💎 $it 💎" })
        list.add(FontItem("dec_wings", "Angel Wings", "Decorative") { "꧁༺ $it ༻꧂" })
        list.add(FontItem("dec_swirl", "Ornate Swirls", "Decorative") { "๛ $it ๛" })
        list.add(FontItem("dec_royal_seal", "Royal Shield", "Decorative", isBold = true) { "🛡️ $it 🛡️" })

        // --- 7. Cursive ---
        list.add(FontItem("cursive_classic", "Classic Cursive", "Cursive", fontFamilyType = "cursive", isItalic = true) { toScript(it) })
        list.add(FontItem("cursive_bold", "Bold Luxury Cursive", "Cursive", fontFamilyType = "cursive", isBold = true, isItalic = true) { toBoldScript(it) })
        list.add(FontItem("cursive_soft", "Soft Ribbon Cursive", "Cursive", fontFamilyType = "cursive", isItalic = true) { "🎀 " + toScript(it) })
        list.add(FontItem("cursive_love", "Romantic Love Cursive", "Cursive", fontFamilyType = "cursive", isBold = true, isItalic = true) { "❥ " + toBoldScript(it) + " ❥" })

        // --- 8. Pixel ---
        list.add(FontItem("pixel_8bit", "8-Bit Game Text", "Pixel", fontFamilyType = "monospace") { "🎮 " + toFullwidth(it) })
        list.add(FontItem("pixel_arcade", "Arcade Retro Grid", "Pixel", fontFamilyType = "monospace") { "🕹️ " + toSquared(it) })
        list.add(FontItem("pixel_gameboy", "GameBoy Classic", "Pixel", fontFamilyType = "monospace", isBold = true) { "【 $it 】" })
        list.add(FontItem("pixel_matrix", "Matrix Hacker", "Pixel", fontFamilyType = "monospace") { "👾 " + toMonospace(it) })

        // --- 9. Retro ---
        list.add(FontItem("retro_vaporwave", "Vaporwave Aesthetic 1980", "Retro") { toFullwidth(it) })
        list.add(FontItem("retro_cassette", "Retro Cassette 90s", "Retro") { "📼 " + toFullwidth(it) })
        list.add(FontItem("retro_disco", "Disco 70s Glow", "Retro", isItalic = true) { "🪩 " + toBoldScript(it) })
        list.add(FontItem("retro_synth", "Synthwave Synth", "Retro", isBold = true) { "⚡ " + toSansBold(it) + " ⚡" })

        // --- 10. Modern ---
        list.add(FontItem("mod_minimal_caps", "Clean Modern All-Caps", "Modern", fontFamilyType = "sans_serif") { it.uppercase().map { c -> "$c " }.joinToString("").trim() })
        list.add(FontItem("mod_clean_sans", "Ultra Modern Sans", "Modern", fontFamilyType = "sans_serif") { toSans(it) })
        list.add(FontItem("mod_tech_bold", "Modern Bold Tech", "Modern", fontFamilyType = "sans_serif", isBold = true) { toSansBold(it) })
        list.add(FontItem("mod_slate", "Modern Slate UI", "Modern", fontFamilyType = "sans_serif") { "• $it •" })

        // --- 11. Minimal ---
        list.add(FontItem("min_spaced", "Spaced Minimalist", "Minimal") { it.map { c -> "$c " }.joinToString("").trim() })
        list.add(FontItem("min_low", "Minimalist Lowercase", "Minimal") { it.lowercase() })
        list.add(FontItem("min_dot", "Minimal Dot Divider", "Minimal") { "· " + it.lowercase() + " ·" })
        list.add(FontItem("min_slash", "Minimalist Slash", "Minimal") { "/ $it /" })

        // --- 12. Bold ---
        list.add(FontItem("bold_sans", "Sans Heavy Black", "Bold", fontFamilyType = "sans_serif", isBold = true) { toSansBold(it) })
        list.add(FontItem("bold_serif", "Serif Heavy Black", "Bold", fontFamilyType = "serif", isBold = true) { toSerifBold(it) })
        list.add(FontItem("bold_fraktur", "Heavy Gothic Black", "Bold", fontFamilyType = "serif", isBold = true) { toBoldFraktur(it) })
        list.add(FontItem("bold_bubble", "Bold Inverted Bubble", "Bold", isBold = true) { toCircledDark(it) })
        list.add(FontItem("bold_box", "Bold Dark Box", "Bold", isBold = true) { toSquaredDark(it) })

        // --- 13. Light ---
        list.add(FontItem("light_thin", "Thin Light Sans", "Light", fontFamilyType = "sans_serif") { toSans(it) })
        list.add(FontItem("light_spaced", "Light Air Spaced", "Light") { it.map { c -> "$c  " }.joinToString("").trim() })
        list.add(FontItem("light_sub", "Subscript Light", "Light") { toSubscript(it) })
        list.add(FontItem("light_super", "Superscript Light", "Light") { toSuperscript(it) })

        // --- 14. Condensed ---
        list.add(FontItem("cond_compact", "Compact Tight Letters", "Condensed") { toSmallCaps(it) })
        list.add(FontItem("cond_caps", "Condensed Tight Caps", "Condensed") { it.uppercase().replace(" ", "") })
        list.add(FontItem("cond_bracket", "Condensed Bracketed", "Condensed") { "[$it]" })

        // --- 15. Extended ---
        list.add(FontItem("ext_wide", "Wide Fullwidth Extended", "Extended") { toFullwidth(it) })
        list.add(FontItem("ext_spaced_bold", "Wide Spaced Bold", "Extended", isBold = true) { toSansBold(it).map { c -> "$c " }.joinToString("").trim() })
        list.add(FontItem("ext_banner", "Banner Billboard", "Extended", isBold = true) { "══╡ $it ╞══" })

        // --- 16. Outline ---
        list.add(FontItem("out_double", "Blackboard Outline", "Outline", isBold = true) { toDoubleStruck(it) })
        list.add(FontItem("out_square", "Outlined Boxed Letter", "Outline") { toSquared(it) })
        list.add(FontItem("out_circle", "Outlined Circle Ring", "Outline") { toCircled(it) })

        // --- 17. Shadow ---
        list.add(FontItem("shad_drop", "Drop Shadow Effect", "Shadow") { "░ $it ░" })
        list.add(FontItem("shad_block", "3D Block Shadow", "Shadow", isBold = true) { "▌ $it ▐" })
        list.add(FontItem("shad_double", "Double Shadow", "Shadow", isBold = true) { toDoubleStruck(it) + " ▓" })

        // --- 18. Graffiti ---
        list.add(FontItem("graf_wild", "Wildstyle Graffiti", "Graffiti", isBold = true) { "𝖄𝖔! " + toBoldFraktur(it) + " 💥" })
        list.add(FontItem("graf_tag", "Street Spray Tag", "Graffiti", isBold = true) { "⚡ " + toBoldScript(it) + " ⚡" })
        list.add(FontItem("graf_drip", "Dripping Ink Paint", "Graffiti", isBold = true) { "💧 " + toSansBold(it) + " 💧" })
        list.add(FontItem("graf_bubble", "Bubble Bombing", "Graffiti", isBold = true) { toCircledDark(it) })

        // --- 19. Gothic ---
        list.add(FontItem("goth_fraktur", "Fraktur Old English", "Gothic", fontFamilyType = "serif") { toFraktur(it) })
        list.add(FontItem("goth_bold_fraktur", "Imperial Bold Fraktur", "Gothic", fontFamilyType = "serif", isBold = true) { toBoldFraktur(it) })
        list.add(FontItem("goth_dark", "Dark Kingdom Gothic", "Gothic", fontFamilyType = "serif", isBold = true) { "⚔️ " + toBoldFraktur(it) + " ⚔️" })
        list.add(FontItem("goth_vampire", "Nocturnal Vampire", "Gothic", fontFamilyType = "serif") { "🦇 " + toFraktur(it) + " 🦇" })

        // --- 20. Calligraphy ---
        list.add(FontItem("calli_lux", "Luxury Calligraphy Ribbon", "Calligraphy", fontFamilyType = "cursive", isBold = true, isItalic = true) { "⚜️ " + toBoldScript(it) + " ⚜️" })
        list.add(FontItem("calli_quill", "Feather Quill Pen", "Calligraphy", fontFamilyType = "cursive", isItalic = true) { "🪶 " + toScript(it) })
        list.add(FontItem("calli_scroll", "Ancient Papyrus Scroll", "Calligraphy", fontFamilyType = "serif") { "📜 " + toFraktur(it) + " 📜" })

        // --- 21. Comic ---
        list.add(FontItem("comic_pop", "Comic Book Pop Art", "Comic", isBold = true) { "💥 " + toSansBold(it).uppercase() + " 💥" })
        list.add(FontItem("comic_bubble", "Speech Bubble Chat", "Comic") { "💬 " + it + " 💬" })
        list.add(FontItem("comic_boom", "Superhero Boom Action", "Comic", isBold = true) { "⚡ " + toFullwidth(it) + " ⚡" })

        // --- 22. Tech ---
        list.add(FontItem("tech_cyber", "Cybernetic Terminal", "Tech", fontFamilyType = "monospace") { "⟦ " + toMonospace(it) + " ⟧" })
        list.add(FontItem("tech_binary", "Binary Circuit Node", "Tech", fontFamilyType = "monospace") { "010 " + toMonospace(it) + " 010" })
        list.add(FontItem("tech_chip", "Silicon Microchip", "Tech", fontFamilyType = "monospace", isBold = true) { "⚙️ " + toSansBold(it) + " ⚙️" })

        // --- 23. Futuristic ---
        list.add(FontItem("fut_scifi", "Sci-Fi Starship HUD", "Futuristic", fontFamilyType = "monospace", isBold = true) { "⟨⟨ " + toSansBold(it) + " ⟩⟩" })
        list.add(FontItem("fut_galaxy", "Interstellar Nebula", "Futuristic") { "🚀 " + toFullwidth(it) + " 🌌" })
        list.add(FontItem("fut_cyber", "Cyberpunk 2077 HUD", "Futuristic", fontFamilyType = "monospace", isBold = true) { "◢ " + toSansBold(it) + " ◣" })

        // --- 24. Vintage ---
        list.add(FontItem("vint_typewriter", "Typewriter 1920", "Vintage", fontFamilyType = "monospace") { toMonospace(it) })
        list.add(FontItem("vint_newspaper", "Newspaper Headline 1880", "Vintage", fontFamilyType = "serif", isBold = true) { toSerifBold(it) })
        list.add(FontItem("vint_victorian", "Victorian Elegance", "Vintage", fontFamilyType = "serif", isItalic = true) { "༺ " + toSerif(it) + " ༻" })

        // --- 25. Art Deco ---
        list.add(FontItem("deco_gatsby", "Great Gatsby 1920s", "Art Deco", isBold = true) { "◆ " + toDoubleStruck(it) + " ◆" })
        list.add(FontItem("deco_golden", "Golden Era Luxury", "Art Deco", isBold = true) { "✨ " + toSansBold(it).uppercase() + " ✨" })
        list.add(FontItem("deco_diamond", "Diamond Geometry", "Art Deco") { "◈ " + toSmallCaps(it) + " ◈" })

        // --- 26. Brush ---
        list.add(FontItem("brush_ink", "Asian Ink Brush", "Brush", fontFamilyType = "cursive", isItalic = true) { "🖌️ " + toScript(it) })
        list.add(FontItem("brush_heavy", "Heavy Acrylic Brush", "Brush", isBold = true) { "🎨 " + toSansBold(it) })
        list.add(FontItem("brush_splash", "Paint Splash Modern", "Brush", isBold = true) { "✦ " + toBoldScript(it) + " ✦" })

        // --- 27. Stencil ---
        list.add(FontItem("sten_military", "Military Stencil Box", "Stencil", fontFamilyType = "monospace") { toSquared(it) })
        list.add(FontItem("sten_crate", "Industrial Cargo Crate", "Stencil", fontFamilyType = "monospace") { "[ " + it.uppercase() + " ]" })
        list.add(FontItem("sten_caution", "Caution Hazard Alert", "Stencil", isBold = true) { "⚠️ " + it.uppercase() + " ⚠️" })

        // --- 28. Neon ---
        list.add(FontItem("neon_glow", "Tokyo Neon Light", "Neon", isBold = true) { "🏮 " + toSansBold(it) + " 🏮" })
        list.add(FontItem("neon_night", "Nightclub Neon Sign", "Neon", isBold = true) { "💡 " + toBoldScript(it) + " 💡" })
        list.add(FontItem("neon_bar", "Neon Tube Bar", "Neon") { "✨ " + toFullwidth(it) + " ✨" })

        // --- 29. Sinhala Style Suite (Dynamic & Authentic for Sinhala & Universal Text) ---
        list.add(FontItem("sin_fm_derana", "FM Derana Royal Ornate", "Sinhala", fontFamilyType = "serif", isBold = true) { "꧁༺ $it ༻꧂" })
        list.add(FontItem("sin_fm_abhaya", "FM Abhaya Classic Serif", "Sinhala", fontFamilyType = "serif") { "『 $it 』" })
        list.add(FontItem("sin_bhashitha", "Bhashitha Modern Bold", "Sinhala", isBold = true) { "【 $it 】" })
        list.add(FontItem("sin_sandeshaya", "Sandeshaya Calligraphy", "Sinhala", fontFamilyType = "cursive", isItalic = true) { "⚜️ $it ⚜️" })
        list.add(FontItem("sin_kaputa", "Kaputa Web Tech Code", "Sinhala", fontFamilyType = "monospace") { "[ $it ]" })
        list.add(FontItem("sin_iskoola", "Iskoola Pota Classic", "Sinhala", fontFamilyType = "serif") { "« $it »" })
        list.add(FontItem("sin_malithi", "Malithi Thin Elegance", "Sinhala") { "• $it •" })
        list.add(FontItem("sin_sinha_crown", "Sinha Royal Crown", "Sinhala", isBold = true) { "👑 " + toDoubleUnderline(it) + " 👑" })
        list.add(FontItem("sin_subhasitha", "Subhasitha Artistic Flow", "Sinhala", isItalic = true) { "✨ " + toWavyUnderline(it) + " ✨" })
        list.add(FontItem("sin_helakuru", "Helakuru Heritage", "Sinhala", isBold = true) { "◈ " + toUnderline(it) + " ◈" })
        list.add(FontItem("sin_swarna", "Swarna Golden Stars", "Sinhala") { "✦ $it ✦" })
        list.add(FontItem("sin_gitanjali", "Gitanjali Poetic Blossom", "Sinhala", isItalic = true) { "🌸 $it 🌸" })
        list.add(FontItem("sin_prathiba", "Prathiba Future HUD", "Sinhala", isBold = true) { "◢ $it ◣" })
        list.add(FontItem("sin_anuradhapura", "Anuradhapura Carved Stone", "Sinhala", fontFamilyType = "serif", isBold = true) { "༺ $it ༻" })
        list.add(FontItem("sin_polonnaruwa", "Polonnaruwa Papyrus", "Sinhala", fontFamilyType = "monospace") { "⟦ $it ⟧" })
        list.add(FontItem("sin_sigiriya", "Sigiriya Fresco Lotus", "Sinhala", fontFamilyType = "cursive", isItalic = true) { "✿ $it ✿" })
        list.add(FontItem("sin_lankadeepa", "Lankadeepa Bold Headline", "Sinhala", isBold = true) { "★ $it ★" })
        list.add(FontItem("sin_mawbima", "Mawbima Chronicle", "Sinhala", isBold = true) { "❚❚ $it ❚❚" })
        list.add(FontItem("sin_ruwanveli", "Ruwanveli Sacred Script", "Sinhala", fontFamilyType = "cursive") { "࿐ $it ࿐" })
        list.add(FontItem("sin_diyawanna", "Diyawanna Pearl Underline", "Sinhala") { toUnderline(it) })
        list.add(FontItem("sin_strike", "Sinhala Strikethrough", "Sinhala") { toStrikethrough(it) })
        list.add(FontItem("sin_slash", "Sinhala Slashed", "Sinhala") { toSlashThrough(it) })
        list.add(FontItem("sin_wavy", "Sinhala Wavy Underline", "Sinhala") { toWavyUnderline(it) })
        list.add(FontItem("sin_overline", "Sinhala Overline Horizon", "Sinhala") { toOverline(it) })
        list.add(FontItem("sin_dotted", "Sinhala Dot Accent", "Sinhala") { toDotBelow(it) })

        // Expand further across all categories with tailored aesthetic transformers
        val styles = listOf(
            "Glamour", "Nova", "Cosmic", "Prime", "Ultra", "Elite", "Pro", "Vibe", "Aura", "Pulse",
            "Zenith", "Horizon", "Phantom", "Eclipse", "Onyx", "Crystal", "Radiant", "Titan", "Bliss", "Vortex"
        )
        val catList = CATEGORIES.filter { it != "All" && it != "Custom Imported" }
        var count = list.size
        for (cat in catList) {
            val familyType = when (cat) {
                "Serif" -> "serif"
                "Monospace", "Tech", "Futuristic" -> "monospace"
                "Handwriting", "Cursive", "Calligraphy" -> "cursive"
                "Sans Serif" -> "sans_serif"
                else -> null
            }
            val isBoldCat = cat in listOf("Bold", "Graffiti", "Comic")
            val isItalicCat = cat in listOf("Cursive", "Handwriting", "Calligraphy")

            for ((idx, st) in styles.withIndex()) {
                if (count >= 520) break
                val t = getCategoryTransformer(cat, idx)
                list.add(
                    FontItem(
                        id = "font_var_${cat.lowercase().replace(" ", "_")}_${st.lowercase()}",
                        name = "$st $cat",
                        category = cat,
                        fontFamilyType = familyType,
                        isBold = isBoldCat || idx % 3 == 0,
                        isItalic = isItalicCat || idx % 4 == 1,
                        transformer = t
                    )
                )
                count++
            }
        }
    }

    private fun getCategoryTransformer(category: String, index: Int): ((String) -> String) {
        return when (category) {
            "Pixel" -> {
                val list = listOf<(String) -> String>(
                    { "【 $it 】" },
                    { "🕹️ [ $it ] 🕹️" },
                    { "👾 " + toMonospace(it) },
                    { "▓▒░ $it ░▒▓" },
                    { toFullwidth(it) },
                    { toSlashThrough(toFullwidth(it)) },
                    { toDotAbove(toFullwidth(it)) },
                    { toSquaredDark(it) },
                    { toSquared(it) },
                    { "🎮 $it 🎮" },
                    { "👾 ♥ $it ♥" },
                    { "⟦ $it ⟧" },
                    { "♫ $it ♫" },
                    { toUnderline(it) },
                    { toStrikethrough(it) },
                    { "★ $it ★" }
                )
                list[index % list.size]
            }
            "Sinhala" -> {
                val list = listOf<(String) -> String>(
                    { "꧁༺ $it ༻꧂" },
                    { "『 $it 』" },
                    { "【 $it 】" },
                    { "⚜️ $it ⚜️" },
                    { "[ $it ]" },
                    { "« $it »" },
                    { "• $it •" },
                    { "👑 " + toDoubleUnderline(it) + " 👑" },
                    { "✨ " + toWavyUnderline(it) + " ✨" },
                    { "◈ " + toUnderline(it) + " ◈" },
                    { "✦ $it ✦" },
                    { "🌸 $it 🌸" },
                    { "◢ $it ◣" },
                    { "༺ $it ༻" },
                    { "⟦ $it ⟧" },
                    { "✿ $it ✿" },
                    { "★ $it ★" },
                    { "❚❚ $it ❚❚" },
                    { "࿐ $it ࿐" },
                    { toUnderline(it) },
                    { toStrikethrough(it) },
                    { toSlashThrough(it) },
                    { toWavyUnderline(it) },
                    { toOverline(it) },
                    { toDotBelow(it) }
                )
                list[index % list.size]
            }
            "Retro" -> {
                val list = listOf<(String) -> String>(
                    { toFullwidth(it) },
                    { "📼 " + toFullwidth(it) + " 📼" },
                    { "⚡ " + toSansBold(it) + " ⚡" },
                    { "🪩 " + toBoldScript(it) + " 🪩" },
                    { toSlashThrough(toSansBold(it)) },
                    { "📻 " + toCircled(it) + " 📻" },
                    { "🕹️ " + toSquared(it) + " 🕹️" },
                    { "~* " + toScript(it) + " *~" },
                    { toUnderline(toFullwidth(it)) },
                    { toWavyUnderline(toFullwidth(it)) },
                    { "📺 " + toFullwidth(it) },
                    { "═╡ $it ╞═" },
                    { toStrikethrough(toFullwidth(it)) },
                    { "☮️ " + toScript(it) + " ☮️" },
                    { "💾 " + toMonospace(it) },
                    { "✨ " + toFullwidth(it) + " ✨" }
                )
                list[index % list.size]
            }
            "Gothic" -> {
                val list = listOf<(String) -> String>(
                    { toFraktur(it) },
                    { toBoldFraktur(it) },
                    { "⚔️ " + toBoldFraktur(it) + " ⚔️" },
                    { "🦇 " + toFraktur(it) + " 🦇" },
                    { "✝ " + toBoldFraktur(it) + " ✝" },
                    { "༒ " + toFraktur(it) + " ༒" },
                    { "📜 " + toBoldFraktur(it) + " 📜" },
                    { toStrikethrough(toBoldFraktur(it)) },
                    { toSlashThrough(toFraktur(it)) },
                    { "☠️ " + toBoldFraktur(it) + " ☠️" },
                    { "🗡️ " + toFraktur(it) + " 🗡️" },
                    { "⚜ " + toBoldFraktur(it) + " ⚜" },
                    { "༺ " + toFraktur(it) + " ༻" },
                    { "⛓️ " + toBoldFraktur(it) + " ⛓️" },
                    { toUnderline(toBoldFraktur(it)) },
                    { "🖤 " + toFraktur(it) + " 🖤" }
                )
                list[index % list.size]
            }
            "Handwriting", "Cursive", "Calligraphy" -> {
                val list = listOf<(String) -> String>(
                    { toScript(it) },
                    { toBoldScript(it) },
                    { "🎀 " + toScript(it) + " 🎀" },
                    { "❥ " + toBoldScript(it) + " ❥" },
                    { "✍️ " + toScript(it) },
                    { "📖 " + toBoldScript(it) },
                    { "꧁༺ " + toScript(it) + " ༻꧂" },
                    { "⚜️ " + toBoldScript(it) + " ⚜️" },
                    { toWavyUnderline(toScript(it)) },
                    { "🪶 " + toScript(it) },
                    { "📜 " + toBoldScript(it) },
                    { "💌 " + toScript(it) + " 💌" },
                    { "࿐ " + toScript(it) + " ࿐" },
                    { toUnderline(toBoldScript(it)) },
                    { "🌸 " + toScript(it) + " 🌸" },
                    { "✨ " + toBoldScript(it) + " ✨" }
                )
                list[index % list.size]
            }
            "Monospace", "Tech", "Futuristic" -> {
                val list = listOf<(String) -> String>(
                    { toMonospace(it) },
                    { "⟦ " + toMonospace(it) + " ⟧" },
                    { "> " + toMonospace(it) },
                    { "// " + toMonospace(it) },
                    { "[$it]" },
                    { "⟨⟨ " + toSansBold(it) + " ⟩⟩" },
                    { "◢ " + toSansBold(it) + " ◣" },
                    { "01 " + toMonospace(it) + " 10" },
                    { toSlashThrough(toMonospace(it)) },
                    { "<" + toMonospace(it) + "/>" },
                    { "[# " + toMonospace(it) + " #]" },
                    { "⚡ " + toMonospace(it) + " ⚡" },
                    { "/* $it */" },
                    { toUnderline(toMonospace(it)) },
                    { "⚙️ " + toMonospace(it) + " ⚙️" },
                    { "🚀 " + toFullwidth(it) }
                )
                list[index % list.size]
            }
            "Serif" -> {
                val list = listOf<(String) -> String>(
                    { toSerifBold(it) },
                    { toSerifItalic(it) },
                    { toSerifBoldItalic(it) },
                    { "« $it »" },
                    { "👑 " + toSerifBold(it) + " 👑" },
                    { "‹ $it ›" },
                    { toUnderline(toSerifBold(it)) },
                    { "『 $it 』" },
                    { "༺ " + toSerif(it) + " ༻" },
                    { "🏛️ " + toSerifBold(it) },
                    { toDoubleUnderline(toSerifBold(it)) },
                    { "📜 " + toSerifBold(it) + " 📜" },
                    { toWavyUnderline(toSerifBold(it)) },
                    { "• " + toSerifBold(it) + " •" },
                    { toStrikethrough(toSerifBold(it)) },
                    { "✦ " + toSerifBold(it) + " ✦" }
                )
                list[index % list.size]
            }
            "Sans Serif" -> {
                val list = listOf<(String) -> String>(
                    { toSansBold(it) },
                    { toSansItalic(it) },
                    { toSansBoldItalic(it) },
                    { toSans(it) },
                    { it.map { c -> "$c " }.joinToString("").trim() },
                    { toUnderline(toSansBold(it)) },
                    { "• $it •" },
                    { toDoubleUnderline(toSansBold(it)) },
                    { toStrikethrough(toSansBold(it)) },
                    { "/ $it /" },
                    { "❚ $it ❚" },
                    { toDotBelow(toSansBold(it)) },
                    { toOverline(toSansBold(it)) },
                    { toWavyUnderline(toSansBold(it)) },
                    { "· $it ·" },
                    { "◆ $it ◆" }
                )
                list[index % list.size]
            }
            "Bold" -> {
                val list = listOf<(String) -> String>(
                    { toSansBold(it) },
                    { toSerifBold(it) },
                    { toBoldFraktur(it) },
                    { toCircledDark(it) },
                    { toSquaredDark(it) },
                    { "【 $it 】" },
                    { "❚❚ $it ❚❚" },
                    { toDoubleUnderline(toSansBold(it)) },
                    { "★ " + toSansBold(it) + " ★" },
                    { "▌ $it ▐" },
                    { toUnderline(toSansBold(it)) },
                    { "💥 " + toSansBold(it) + " 💥" },
                    { "⚡ " + toSansBold(it) + " ⚡" },
                    { "▓ " + toSansBold(it) + " ▓" },
                    { "【 " + toSerifBold(it) + " 】" },
                    { "🛡️ " + toSansBold(it) + " 🛡️" }
                )
                list[index % list.size]
            }
            "Light" -> {
                val list = listOf<(String) -> String>(
                    { toSans(it) },
                    { it.map { c -> "$c  " }.joinToString("").trim() },
                    { toSubscript(it) },
                    { toSuperscript(it) },
                    { "· " + it.lowercase() + " ·" },
                    { toDotAbove(it) },
                    { "• $it •" },
                    { toOverline(it) },
                    { toWavyUnderline(it) },
                    { "/ $it /" },
                    { "‹ $it ›" },
                    { "✧ $it ✧" },
                    { "☁️ $it ☁️" },
                    { "· $it ·" },
                    { "░ $it ░" },
                    { "⋆ $it ⋆" }
                )
                list[index % list.size]
            }
            "Condensed" -> {
                val list = listOf<(String) -> String>(
                    { toSmallCaps(it) },
                    { it.uppercase().replace(" ", "") },
                    { "[$it]" },
                    { "|$it|" },
                    { "⟦$it⟧" },
                    { "『" + toSmallCaps(it) + "』" },
                    { "【$it】" },
                    { toUnderline(toSmallCaps(it)) },
                    { "《$it》" },
                    { "‹$it›" },
                    { "«$it»" },
                    { "❚$it❚" },
                    { toStrikethrough(toSmallCaps(it)) },
                    { "❲$it❳" },
                    { "〔$it〕" }
                )
                list[index % list.size]
            }
            "Extended" -> {
                val list = listOf<(String) -> String>(
                    { toFullwidth(it) },
                    { toSansBold(it).map { c -> "$c " }.joinToString("").trim() },
                    { "══╡ $it ╞══" },
                    { it.map { c -> "$c   " }.joinToString("").trim() },
                    { "— $it —" },
                    { "═ $it ═" },
                    { "•— $it —•" },
                    { toUnderline(toFullwidth(it)) },
                    { toDoubleUnderline(toFullwidth(it)) },
                    { "★═ $it ═★" },
                    { "◈═ $it ═◈" },
                    { "✦ " + toFullwidth(it) + " ✦" },
                    { "《 " + toFullwidth(it) + " 》" },
                    { "░▒ " + toFullwidth(it) + " ▒░" },
                    { "▓ " + toFullwidth(it) + " ▓" },
                    { "═╡ " + toFullwidth(it) + " ╞═" }
                )
                list[index % list.size]
            }
            "Outline" -> {
                val list = listOf<(String) -> String>(
                    { toDoubleStruck(it) },
                    { toSquared(it) },
                    { toCircled(it) },
                    { "⟦ " + toDoubleStruck(it) + " ⟧" },
                    { "❨ $it ❩" },
                    { "[ $it ]" },
                    { "⌠ $it ⌡" },
                    { "『 $it 』" },
                    { toUnderline(toDoubleStruck(it)) },
                    { "◇ $it ◇" },
                    { "◈ $it ◈" },
                    { "◻ $it ◻" },
                    { "○ $it ○" },
                    { "« " + toDoubleStruck(it) + " »" },
                    { "✦ " + toDoubleStruck(it) + " ✦" },
                    { "❲ " + toDoubleStruck(it) + " ❳" }
                )
                list[index % list.size]
            }
            "Shadow" -> {
                val list = listOf<(String) -> String>(
                    { "░ $it ░" },
                    { "▌ $it ▐" },
                    { toDoubleStruck(it) + " ▓" },
                    { "▓▒ $it ▒▓" },
                    { "▞ $it ▚" },
                    { "▊ $it ▋" },
                    { toUnderline(it) + " ░" },
                    { "◢ $it ◣" },
                    { "◤ $it ◥" },
                    { "▪ $it ▪" },
                    { "▍ $it ▍" },
                    { "▒ $it ▒" },
                    { "▓ $it ▓" },
                    { toSlashThrough(it) + " ▌" },
                    { "░▒▓ $it ▓▒░" },
                    { "▌ " + toSansBold(it) + " ▐" }
                )
                list[index % list.size]
            }
            "Graffiti" -> {
                val list = listOf<(String) -> String>(
                    { "𝖄𝖔! " + toBoldFraktur(it) + " 💥" },
                    { "⚡ " + toBoldScript(it) + " ⚡" },
                    { "💧 " + toSansBold(it) + " 💧" },
                    { toCircledDark(it) },
                    { "🔥 " + toBoldFraktur(it) + " 🔥" },
                    { "☠️ " + toSansBold(it) + " ☠️" },
                    { toSlashThrough(toBoldFraktur(it)) },
                    { "✦ " + toBoldScript(it) + " ✦" },
                    { "🛹 " + toSansBold(it) },
                    { "★ " + toBoldFraktur(it) + " ★" },
                    { "💣 " + toSansBold(it) + " 💣" },
                    { "ıllıllı $it ıllıllı" },
                    { "★彡 $it 彡★" },
                    { "👑 " + toBoldFraktur(it) + " 👑" },
                    { toStrikethrough(toBoldFraktur(it)) },
                    { "🎨 " + toBoldScript(it) }
                )
                list[index % list.size]
            }
            "Comic" -> {
                val list = listOf<(String) -> String>(
                    { "💥 " + toSansBold(it).uppercase() + " 💥" },
                    { "💬 $it 💬" },
                    { "⚡ " + toFullwidth(it) + " ⚡" },
                    { "🗯️ $it 🗯️" },
                    { "POW! " + toSansBold(it) },
                    { "ZAP! " + toFullwidth(it) },
                    { "🦸 " + toSansBold(it) },
                    { "⭐ " + toSquared(it) + " ⭐" },
                    { "✨ " + toSansBold(it).uppercase() + " ✨" },
                    { "【 " + toSansBold(it).uppercase() + " 】" },
                    { "🔥 " + toSansBold(it) + " 🔥" },
                    { "🎭 $it 🎭" },
                    { "BOOM! $it" },
                    { "★ " + toSansBold(it) + " ★" },
                    { "🃏 $it 🃏" },
                    { "🚀 " + toSansBold(it) + " 🚀" }
                )
                list[index % list.size]
            }
            "Neon" -> {
                val list = listOf<(String) -> String>(
                    { "🏮 " + toSansBold(it) + " 🏮" },
                    { "💡 " + toBoldScript(it) + " 💡" },
                    { "✨ " + toFullwidth(it) + " ✨" },
                    { "⚡ " + toSansBold(it) + " ⚡" },
                    { "🔮 " + toDoubleStruck(it) + " 🔮" },
                    { "🌟 " + toFullwidth(it) + " 🌟" },
                    { "🚥 $it 🚥" },
                    { "🌆 " + toSansBold(it) + " 🌆" },
                    { "✨ " + toUnderline(it) + " ✨" },
                    { "💫 " + toBoldScript(it) + " 💫" },
                    { "🎆 " + toFullwidth(it) + " 🎆" },
                    { "✦ " + toSansBold(it) + " ✦" },
                    { "• " + toFullwidth(it) + " •" },
                    { "⭐ " + toSansBold(it) + " ⭐" },
                    { "💠 " + toDoubleStruck(it) + " 💠" },
                    { "🌃 $it 🌃" }
                )
                list[index % list.size]
            }
            "Decorative" -> {
                val list = listOf<(String) -> String>(
                    { "✨ $it ✨" },
                    { "❤️ $it ❤️" },
                    { "★ $it ★" },
                    { "🎵 $it 🎶" },
                    { "👑 $it 👑" },
                    { "🦋 $it 🦋" },
                    { "🔥 $it 🔥" },
                    { "🌸 $it 🌸" },
                    { "💎 $it 💎" },
                    { "꧁༺ $it ༻꧂" },
                    { "๛ $it ๛" },
                    { "🛡️ $it 🛡️" },
                    { "✿ $it ✿" },
                    { "༒ $it ༒" },
                    { "࿐ $it ࿐" },
                    { "◈ $it ◈" }
                )
                list[index % list.size]
            }
            else -> {
                val list = listOf<(String) -> String>(
                    { toSansBold(it) },
                    { toSansItalic(it) },
                    { toSerifBold(it) },
                    { toScript(it) },
                    { toBoldScript(it) },
                    { toFraktur(it) },
                    { toBoldFraktur(it) },
                    { toDoubleStruck(it) },
                    { toSmallCaps(it) },
                    { toCircled(it) },
                    { toCircledDark(it) },
                    { toSquared(it) },
                    { toSquaredDark(it) },
                    { toFullwidth(it) },
                    { toMonospace(it) },
                    { toUnderline(it) },
                    { toWavyUnderline(it) },
                    { toStrikethrough(it) }
                )
                list[index % list.size]
            }
        }
    }

    // --- Unicode String Transformation Algorithms ---

    fun toSans(text: String): String = mapCharacters(text, 'A'..'Z', 0x1D5A0, 'a'..'z', 0x1D5BA, '0'..'9', 0x1D7E2)

    fun toSansBold(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D5D4, 'a'..'z', 0x1D5EE, '0'..'9', 0x1D7EC)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "• $text •" else res
    }

    fun toSansItalic(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D608, 'a'..'z', 0x1D622)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "/ $text /" else res
    }

    fun toSansBoldItalic(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D63C, 'a'..'z', 0x1D656)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "❚ $text ❚" else res
    }

    fun toSerif(text: String): String {
        return if (text.isNotEmpty() && !text.all { it.isWhitespace() }) "« $text »" else text
    }

    fun toSerifBold(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D400, 'a'..'z', 0x1D41A, '0'..'9', 0x1D7CE)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "« $text »" else res
    }

    fun toSerifItalic(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D434, 'a'..'z', 0x1D44E)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "‹ $text ›" else res
    }

    fun toSerifBoldItalic(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D468, 'a'..'z', 0x1D482)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "«‹ $text ›»" else res
    }

    fun toScript(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D49C, 'a'..'z', 0x1D4B6)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "~* $text *~" else res
    }

    fun toBoldScript(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D4D0, 'a'..'z', 0x1D4EA)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "⚜️ $text ⚜️" else res
    }

    fun toFraktur(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D504, 'a'..'z', 0x1D51E)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "༺ $text ༻" else res
    }

    fun toBoldFraktur(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D56C, 'a'..'z', 0x1D586)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "⚔️ $text ⚔️" else res
    }

    fun toDoubleStruck(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D538, 'a'..'z', 0x1D552, '0'..'9', 0x1D7D8)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "⟦ $text ⟧" else res
    }

    fun toMonospace(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1D670, 'a'..'z', 0x1D68A, '0'..'9', 0x1D7F6)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "// $text" else res
    }

    fun toFullwidth(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0xFF21, 'a'..'z', 0xFF41, '0'..'9', 0xFF10)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) text.map { "$it " }.joinToString("").trim() else res
    }

    fun toCircled(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x24B6, 'a'..'z', 0x24D0, '1'..'9', 0x2460)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "❨ $text ❩" else res
    }

    fun toCircledDark(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1F150, 'a'..'z', 0x1F150)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "● $text ●" else res
    }

    fun toSquared(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1F130, 'a'..'z', 0x1F130)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "[ $text ]" else res
    }

    fun toSquaredDark(text: String): String {
        val res = mapCharacters(text, 'A'..'Z', 0x1F170, 'a'..'z', 0x1F170)
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "【 $text 】" else res
    }

    fun toUnderline(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0332" }.joinToString("")

    fun toDoubleUnderline(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0333" }.joinToString("")

    fun toWavyUnderline(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0330" }.joinToString("")

    fun toStrikethrough(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0336" }.joinToString("")

    fun toSlashThrough(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0337" }.joinToString("")

    fun toOverline(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0305" }.joinToString("")

    fun toDotAbove(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0307" }.joinToString("")

    fun toDotBelow(text: String): String =
        text.map { if (it.isWhitespace()) "$it" else "$it\u0323" }.joinToString("")

    fun toSmallCaps(text: String): String {
        val map = mapOf(
            'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ", 'f' to "ꜰ", 'g' to "ɢ",
            'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ", 'k' to "ᴋ", 'l' to "ʟ", 'm' to "ᴍ", 'n' to "ɴ",
            'o' to "ᴏ", 'p' to "ᴘ", 'q' to "ꞯ", 'r' to "ʀ", 's' to "ꜱ", 't' to "ᴛ", 'u' to "ᴜ",
            'v' to "ᴠ", 'w' to "ᴡ", 'x' to "x", 'y' to "ʏ", 'z' to "ᴢ"
        )
        val res = text.map { map[it.lowercaseChar()] ?: it.toString() }.joinToString("")
        return if (res == text && text.isNotEmpty() && !text.all { it.isWhitespace() }) "『 $text 』" else res
    }

    fun toSuperscript(text: String): String {
        val map = mapOf(
            '0' to "⁰", '1' to "¹", '2' to "²", '3' to "³", '4' to "⁴", '5' to "⁵", '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹",
            'a' to "ᵃ", 'b' to "ᵇ", 'c' to "ᶜ", 'd' to "ᵈ", 'e' to "ᵉ", 'f' to "ᶠ", 'g' to "ᵍ", 'h' to "ʰ", 'i' to "ⁱ", 'j' to "ʲ",
            'k' to "ᵏ", 'l' to "ˡ", 'm' to "ᵐ", 'n' to "ⁿ", 'o' to "ᵒ", 'p' to "ᵖ", 'r' to "ʳ", 's' to "ˢ", 't' to "ᵗ", 'u' to "ᵘ",
            'v' to "ᵛ", 'w' to "ʷ", 'x' to "ˣ", 'y' to "ʸ", 'z' to "ᶻ"
        )
        return text.map { map[it.lowercaseChar()] ?: it.toString() }.joinToString("")
    }

    fun toSubscript(text: String): String {
        val map = mapOf(
            '0' to "₀", '1' to "₁", '2' to "₂", '3' to "₃", '4' to "₄", '5' to "₅", '6' to "₆", '7' to "₇", '8' to "₈", '9' to "₉",
            'a' to "ₐ", 'e' to "ₑ", 'h' to "ₕ", 'i' to "ᵢ", 'j' to "ⱼ", 'k' to "ₖ", 'l' to "ₗ", 'm' to "ₘ", 'n' to "ₙ", 'o' to "ₒ",
            'p' to "ₚ", 'r' to "ᵣ", 's' to "ₛ", 't' to "ₜ", 'u' to "ᵤ", 'v' to "ᵥ", 'x' to "ₓ"
        )
        return text.map { map[it.lowercaseChar()] ?: it.toString() }.joinToString("")
    }

    private fun mapCharacters(
        text: String,
        upperRange: CharRange,
        upperBase: Int,
        lowerRange: CharRange? = null,
        lowerBase: Int = 0,
        digitRange: CharRange? = null,
        digitBase: Int = 0
    ): String {
        val sb = StringBuilder()
        for (c in text) {
            when {
                c in upperRange -> sb.append(String(Character.toChars(upperBase + (c - upperRange.first))))
                lowerRange != null && c in lowerRange -> sb.append(String(Character.toChars(lowerBase + (c - lowerRange.first))))
                digitRange != null && c in digitRange -> sb.append(String(Character.toChars(digitBase + (c - digitRange.first))))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
