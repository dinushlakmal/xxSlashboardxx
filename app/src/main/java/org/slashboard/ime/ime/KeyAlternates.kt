package org.slashboard.ime.ime

import org.slashboard.ime.engine.InputMode

/** Long-press extras. Base glyph is prepended by the keyboard, Gboard-style. */
internal object KeyAlternates {
    fun extras(identity: String, mode: InputMode, layer: KeyboardLayer, shifted: Boolean): List<Pair<String, String>> {
        punctuation(identity)?.let { return it.map { value -> value to value } }
        if (layer == KeyboardLayer.LETTERS) latin(identity, shifted)?.let { return it.map { value -> value to value } }
        if (layer == KeyboardLayer.NUMBERS || layer == KeyboardLayer.SYMBOLS || layer == KeyboardLayer.SINHALA_GLYPHS) {
            astrologyOrSinhala(identity)?.let { return it.map { value -> value to value } }
            numbers(identity)?.let { return it.map { value -> value to value } }
        }
        return emptyList()
    }

    private fun astrologyOrSinhala(identity: String): List<String>? = when (identity) {
        "♈" -> listOf("මේෂ", "රවි", "☀️")
        "♉" -> listOf("වෘෂභ", "සඳු", "🌙")
        "♊" -> listOf("මිථුන", "කුජ", "♂")
        "♋" -> listOf("කටක", "බුධ", "☿")
        "♌" -> listOf("සිංහ", "ගුරු", "♃")
        "♍" -> listOf("කන්‍යා", "සිකුරු", "♀")
        "♎" -> listOf("තුලා", "ශනි", "♄")
        "♏" -> listOf("වෘශ්චික", "රාහු", "☊")
        "♐" -> listOf("ධනු", "කේතු", "☋")
        "♑" -> listOf("මකර", "හෝරා")
        "♒" -> listOf("කුම්භ", "දිනය")
        "♓" -> listOf("මීන", "යෝග")
        "෴" -> listOf("තිථි", "නැකත", "යෝග", "කරණ", "හෝරා", "දිනය", "෵")
        "𑇡" -> listOf("෧", "𑇫", "𑇳", "𑇴")
        "𑇢" -> listOf("෨", "𑇬")
        "𑇣" -> listOf("෩", "𑇭")
        "𑇤" -> listOf("෪", "𑇮")
        "𑇥" -> listOf("෫", "𑇯")
        "𑇦" -> listOf("෬", "𑇰")
        "𑇧" -> listOf("෭", "𑇱")
        "𑇨" -> listOf("෮", "𑇲")
        "𑇩" -> listOf("෯", "𑇳")
        "𑇪" -> listOf("෦", "𑇴")
        "෦" -> listOf("෧", "෨", "෩", "෪", "෫", "෬", "෭", "෮", "෯")
        else -> null
    }

    fun hint(identity: String, mode: InputMode, layer: KeyboardLayer): String? {
        return null
    }

    private fun punctuation(identity: String): List<String>? = when (identity) {
        "." -> listOf(",", ";", ":", "?", "!", "…", "෴")
        "," -> listOf(";", ":", "،")
        "'" -> listOf("‘", "’", "\"")
        "\"" -> listOf("“", "”", "'")
        "?" -> listOf("!", "…", "෴")
        "!" -> listOf("¡", "෴")
        "-" -> listOf("–", "—", "•")
        "[" -> listOf("{", "<")
        "]" -> listOf("}", ">")
        else -> null
    }

    private fun latin(identity: String, shifted: Boolean): List<String>? {
        val values = when (identity.lowercase()) {
            "a" -> listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā")
            "e" -> listOf("è", "é", "ê", "ë", "ē", "ė", "ę")
            "i" -> listOf("ì", "í", "î", "ï", "ī")
            "o" -> listOf("ò", "ó", "ô", "ö", "ø", "õ", "œ", "ō")
            "u" -> listOf("ù", "ú", "û", "ü", "ū")
            "c" -> listOf("ç", "ć", "č")
            "n" -> listOf("ñ", "ń")
            "s" -> listOf("ß", "ś", "š")
            "y" -> listOf("ÿ")
            else -> null
        } ?: return null
        return if (shifted) values.map { it.uppercase() } else values
    }

    private fun numbers(identity: String): List<String>? = when (identity) {
        "1" -> listOf("¹", "½", "⅓", "¼")
        "2" -> listOf("²", "⅔")
        "3" -> listOf("³", "¾")
        "0" -> listOf("⁰", "∅", "º")
        "$" -> listOf("¢", "£", "€", "¥", "₨")
        else -> null
    }
}
