package org.slashboard.ime.engine

object FmConverter {
    fun convert(unicode: String): String {
        var text = unicode
        // Standard vowels
        val vowels = mapOf("අ" to "w", "ආ" to "w`", "ඇ" to "we", "ඈ" to "wE", "ඉ" to "b", "ඊ" to "B", "උ" to "W", "ඌ" to "W`", "එ" to "t", "ඒ" to "T", "ඓ" to "ff", "ඔ" to "T", "ඕ" to "´", "ඖ" to "T!au")
        val consonants = mapOf(
            "ක" to "l", "ඛ" to "L", "ග" to ".", "ඝ" to ">", "ඞ" to "X", "ඟ" to "Õ",
            "ච" to "p", "ඡ" to "P", "ජ" to "c", "ඣ" to "C", "ඤ" to "Q",
            "ට" to "g", "ඨ" to "G", "ඩ" to "v", "ඪ" to "V", "ණ" to "K",
            "ත" to ";", "ථ" to ":", "ද" to "o", "ධ" to "O", "න" to "k", "ඳ" to "`o",
            "ප" to "m", "ඵ" to "M", "බ" to "n", "භ" to "N", "ම" to "u", "ඹ" to "A",
            "ය" to "h", "ර" to "r", "ල" to "f", "ව" to "j",
            "ශ" to "Y", "ෂ" to "I", "ස" to "i", "හ" to "y", "ළ" to "<", "ෆ" to "*,"
        )
        // Basic mapping as placeholder. A real FM converter is much more complex
        // due to dependent vowels (pili) taking up different positions.
        for ((u, fm) in consonants) text = text.replace(u, fm)
        for ((u, fm) in vowels) text = text.replace(u, fm)
        return text
    }
}
