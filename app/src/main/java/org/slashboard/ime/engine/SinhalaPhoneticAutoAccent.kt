package org.slashboard.ime.engine

/**
 * Intelligent phonetic auto-accentuation and short/long vowel disambiguation for Sinhala Singlish.
 * Resolves ambiguities when users type 'a' instead of 'aa', 'o' instead of 'oo', 'e' instead of 'ee', etc.
 * Uses contextual cues (preceding words like "මට", "බත්", "කන්න") to select the grammatically
 * and culturally appropriate word (e.g., "mata bath kanna one" -> "මට බත් කන්න ඕනෙ").
 */
object SinhalaPhoneticAutoAccent {

    // Common modal verbs that follow infinitive verbs ending in "න්න" or personal pronouns like "මට", "අපිට"
    private val infinitiveModalFollowers = mapOf(
        "ඔනෙ" to "ඕනෙ",
        "ඔනේ" to "ඕනේ",
        "ඔන" to "ඕන",
        "එප" to "එපා",
        "පුලුවන්" to "පුළුවන්",
        "බහ" to "බැහැ",
        "බැ" to "බෑ",
        "නහ" to "නැහැ",
        "නැ" to "නෑ",
        "ආස" to "ආසයි",
        "අස" to "ආසයි"
    )

    // Frequent colloquial words typed with short vowels in Singlish
    private val commonColloquialAccents = mapOf(
        "ඔනෙ" to "ඕනෙ",
        "ඔනේ" to "ඕනේ",
        "ඔන" to "ඕන",
        "එප" to "එපා",
        "අම්ම" to "අම්මා",
        "ඔය" to "ඔයා",
        "ඔයට" to "ඔයාට",
        "අපිට" to "අපිට",
        "එය" to "එයා",
        "එයට" to "එයාට",
        "අව" to "ආවා",
        "අවා" to "ආවා",
        "අවෙ" to "ආවේ",
        "අවෙමි" to "ආවෙමි",
        "අයෙ" to "ආයේ",
        "අයෙත්" to "ආයෙත්",
        "අදරය" to "ආදරය",
        "අදරෙන්" to "ආදරෙන්",
        "අදරයෙන්" to "ආදරයෙන්",
        "අස" to "ආස",
        "අසයි" to "ආසයි",
        "අගම" to "ආගම",
        "අගමික" to "ආගමික",
        "අයුබොවන්" to "ආයුබෝවන්",
        "අයුබෝවන්" to "ආයුබෝවන්",
        "අහර" to "ආහාර",
        "අහරය" to "ආහාරය",
        "අරක්ෂව" to "ආරක්ෂාව",
        "අරක්ෂාව" to "ආරක්ෂාව",
        "තම" to "තමා",
        "තවම" to "තවම",
        "දැන්" to "දැන්",
        "දන්" to "දැන්",
        "හරි" to "හරි",
        "හොද" to "හොඳ",
        "හොදට" to "හොඳට"
    )

    /**
     * Generates short <-> long vowel variants for a given transliterated Sinhala word.
     */
    fun generateAccentVariants(word: String): List<String> {
        if (word.isEmpty()) return emptyList()
        val variants = LinkedHashSet<String>()

        // 1. Direct dictionary / colloquial lookup
        commonColloquialAccents[word]?.let { variants.add(it) }

        // 2. Initial vowel alternation (short <-> long)
        val initialSwaps = listOf(
            "අ" to "ආ", "ආ" to "අ",
            "ඇ" to "ඈ", "ඈ" to "ඇ",
            "ඉ" to "ඊ", "ඊ" to "ඉ",
            "උ" to "ඌ", "ඌ" to "උ",
            "එ" to "ඒ", "ඒ" to "එ",
            "ඔ" to "ඕ", "ඕ" to "ඔ"
        )
        for ((from, to) in initialSwaps) {
            if (word.startsWith(from)) {
                variants.add(to + word.substring(from.length))
            }
        }

        // 3. Final vowel sign elongation (e.g. consonant + no vowel sign -> add 'ා' or 'ෙ' -> 'ේ')
        if (word.isNotEmpty()) {
            val lastChar = word.last()
            // If ends with consonant without pillam, try adding 'ා' (aelapilla)
            if (SinhalaEngine.isSinhalaConsonant(lastChar.toString())) {
                variants.add(word + "ා")
            } else when (lastChar) {
                'ෙ' -> variants.add(word.dropLast(1) + "ේ")
                'ො' -> variants.add(word.dropLast(1) + "ෝ")
                'ැ' -> variants.add(word.dropLast(1) + "ෑ")
                'ි' -> variants.add(word.dropLast(1) + "ී")
                'ු' -> variants.add(word.dropLast(1) + "ූ")
                'ේ' -> variants.add(word.dropLast(1) + "ෙ")
                'ෝ' -> variants.add(word.dropLast(1) + "ො")
                'ා' -> variants.add(word.dropLast(1))
            }
        }

        // 4. Medial vowel variations (e.g. 'ෙ' -> 'ේ', 'ො' -> 'ෝ')
        if (word.contains("ෙ")) {
            variants.add(word.replace("ෙ", "ේ"))
        }
        if (word.contains("ො")) {
            variants.add(word.replace("ො", "ෝ"))
        }

        variants.remove(word)
        return variants.toList()
    }

    /**
     * Determines whether an auto-accent correction should be applied in the current typing context.
     * E.g. context: ["මට", "බත්", "කන්න"], word: "ඔනෙ" -> returns "ඕනෙ".
     */
    fun findBestAccentCorrection(
        word: String,
        preceding: List<String>,
        frequencyLookup: (String) -> Int?
    ): String? {
        if (word.isEmpty()) return null
        val prev = preceding.lastOrNull().orEmpty()

        // Context check 1: Follows an infinitive verb (ends with "න්න" e.g. "කන්න", "යන්න", "කරන්න")
        // or a dative pronoun (ends with "ට" e.g. "මට", "අපිට", "ඔයාට")
        val isInfinitiveOrPronoun = prev.endsWith("න්න") || prev.endsWith("ට") || prev in setOf("බත්", "කෑම", "ගෙදර", "වැඩ")
        if (isInfinitiveOrPronoun) {
            infinitiveModalFollowers[word]?.let { return it }
        }

        // Context check 2: Colloquial direct check if mapped
        val directColloquial = commonColloquialAccents[word]
        if (directColloquial != null && directColloquial != word) {
            val directFreq = frequencyLookup(directColloquial) ?: 1000
            val currentFreq = frequencyLookup(word) ?: 0
            if (directFreq >= currentFreq || currentFreq < 1500) {
                return directColloquial
            }
        }

        // Context check 3: Evaluate generated variants with frequency lookup
        val variants = generateAccentVariants(word)
        val currentFreq = frequencyLookup(word) ?: 0

        var bestVariant: String? = null
        var bestScore = currentFreq.toDouble()

        for (variant in variants) {
            val freq = frequencyLookup(variant) ?: 0
            // If preceding words match context, boost score
            var score = freq.toDouble()
            if (isInfinitiveOrPronoun && (variant == "ඕනෙ" || variant == "ඕනේ" || variant == "එපා" || variant == "පුළුවන්")) {
                score *= 3.0
            }
            if (score > bestScore && (score > currentFreq * 2 || currentFreq < 500)) {
                bestScore = score
                bestVariant = variant
            }
        }

        return bestVariant
    }
}
