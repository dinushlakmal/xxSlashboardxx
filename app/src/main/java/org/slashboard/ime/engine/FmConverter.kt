package org.slashboard.ime.engine

/**
 * High-accuracy Sinhala Unicode to FM (FM Abhaya / FM Derana / DL font) converter.
 * Properly handles Kombuwa positioning, Diga Kombuwa, Aelapili, Ispili, Papili,
 * Rakaransaya, Bandi Akuru, and Anusvaraya.
 */
object FmConverter {

    private val specialCompounds = listOf(
        "ශ්‍රී" to "YS%S",
        "ක්‍රී" to "l%S",
        "ප්‍රී" to "m%S",
        "ද්‍ර" to "ø",
        "ක්‍ර" to "l%",
        "ප්‍ර" to "m%",
        "ග්‍ර" to ".%",
        "ත්‍ර" to ";%",
        "ධ්‍ර" to "O%",
        "බ්‍ර" to "n%",
        "ශ්‍ර" to "Y%",
        "ස්‍ර" to "i%",
        "ව්‍ර" to "j%",
        "්‍ය" to "H"
    )

    private val vowels = mapOf(
        "අ" to "w", "ආ" to "wd", "ඇ" to "we", "ඈ" to "wE", "ඉ" to "b", "ඊ" to "B",
        "උ" to "W", "ඌ" to "W!", "ඍ" to "c", "ඎ" to "c+", "එ" to "t", "ඒ" to "T",
        "ඓ" to "ff", "ඔ" to "Tda", "ඕ" to "´", "ඖ" to "T!"
    )

    private val consonants = mapOf(
        "ක" to "l", "ඛ" to "L", "ග" to ".", "ඝ" to ">", "ඞ" to "X", "ඟ" to "Õ",
        "ච" to "p", "ඡ" to "P", "ජ" to "c", "ඣ" to "C", "ඤ" to "Q", "ඥ" to "{", "ඦ" to "Ù",
        "ට" to "g", "ඨ" to "G", "ඩ" to "v", "ඪ" to "V", "ණ" to "K", "ඬ" to "Ë",
        "ත" to ";", "ථ" to ":", "ද" to "o", "ධ" to "O", "න" to "k", "ඳ" to "o",
        "ප" to "m", "ඵ" to "M", "බ" to "n", "භ" to "N", "ම" to "u", "ඹ" to "A",
        "ය" to "h", "ර" to "r", "ල" to "f", "ව" to "j", "ළ" to "<", "ෆ" to "*,",
        "ශ" to "Y", "ෂ" to "I", "ස" to "i", "හ" to "y"
    )

    private fun isSinhalaPilla(ch: Char): Boolean {
        val cp = ch.code
        return (cp in 0x0DCF..0x0DDF) || cp == 0x0DCA || cp == 0x0D82 || cp == 0x0D83 ||
                cp == 0x0DF2 || cp == 0x0DF3 || cp == 0x200D || cp == 0x200C
    }

    fun convert(unicode: String): String {
        var res = unicode
        for ((u, fm) in specialCompounds) {
            res = res.replace(u, fm)
        }

        val out = StringBuilder()
        var i = 0
        val n = res.length

        while (i < n) {
            // Standalone vowels check
            var matchedVowel: Pair<String, String>? = null
            for ((v, fm) in vowels) {
                if (res.startsWith(v, i)) {
                    matchedVowel = v to fm
                    break
                }
            }
            if (matchedVowel != null) {
                out.append(matchedVowel.second)
                i += matchedVowel.first.length
                continue
            }

            // Consonants check
            var matchedConsonant: Pair<String, String>? = null
            for ((c, fm) in consonants) {
                if (res.startsWith(c, i)) {
                    matchedConsonant = c to fm
                    break
                }
            }

            if (matchedConsonant != null) {
                val (cChar, cFm) = matchedConsonant
                i += cChar.length

                val pili = StringBuilder()
                while (i < n && isSinhalaPilla(res[i])) {
                    pili.append(res[i])
                    i++
                }
                val pStr = pili.toString()

                when {
                    "්" in pStr -> out.append(cFm).append("a")
                    "ේ" in pStr || ("ෙ" in pStr && "්" in pStr) -> out.append("f").append(cFm).append("a")
                    "ෝ" in pStr || ("ෙ" in pStr && "ා" in pStr && "්" in pStr) || ("ො" in pStr && "්" in pStr) -> out.append("f").append(cFm).append("da")
                    "ො" in pStr || ("ෙ" in pStr && "ා" in pStr) -> out.append("f").append(cFm).append("d")
                    "ෛ" in pStr -> out.append("ff").append(cFm)
                    "ෞ" in pStr || ("ෙ" in pStr && "ෟ" in pStr) -> out.append("f").append(cFm).append("!")
                    "ෙ" in pStr -> out.append("f").append(cFm)
                    "ෑ" in pStr -> out.append(cFm).append("E")
                    "ැ" in pStr -> out.append(cFm).append("e")
                    "ී" in pStr -> out.append(cFm).append("S")
                    "ි" in pStr -> out.append(cFm).append("s")
                    "ූ" in pStr -> {
                        when (cChar) {
                            "ද" -> out.append("¥")
                            "ර" -> out.append("rE")
                            "ඳ" -> out.append("ø")
                            else -> out.append(cFm).append("+")
                        }
                    }
                    "ු" in pStr -> {
                        when (cChar) {
                            "ද" -> out.append("ÿ")
                            "ර" -> out.append("re")
                            "ඳ" -> out.append("÷")
                            "ග" -> out.append(".=")
                            "ත" -> out.append(";=")
                            "ක" -> out.append("l=")
                            else -> out.append(cFm).append("q")
                        }
                    }
                    "ා" in pStr -> out.append(cFm).append("d")
                    "ෘ" in pStr -> out.append(cFm).append("D")
                    else -> out.append(cFm)
                }

                if ("ං" in pStr) out.append("x")
                if ("ඃ" in pStr) out.append("H")
                continue
            }

            if (res[i] == 'ං') {
                out.append("x")
                i++
                continue
            }
            if (res[i] == 'ඃ') {
                out.append("H")
                i++
                continue
            }

            out.append(res[i])
            i++
        }

        return out.toString()
    }
}
