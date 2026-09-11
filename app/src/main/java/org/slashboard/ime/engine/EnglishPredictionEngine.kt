package org.slashboard.ime.engine

import android.content.Context
import org.slashboard.ime.data.Candidate
import org.slashboard.ime.data.LocalLearningStore
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

class EnglishPredictionEngine(
    private val context: Context,
    private val learning: LocalLearningStore
) {
    private val unigramIndex = HashMap<String, Int>(4096)
    private val wordList = ArrayList<Pair<String, Int>>(4096)
    private val wordsByFirstChar = HashMap<Char, ArrayList<Pair<String, Int>>>(32)
    private val phraseBigrams = HashMap<String, MutableList<Pair<String, Int>>>(1024)
    private val phraseTrigrams = HashMap<String, MutableList<Pair<String, Int>>>(512)
    private val typoCorrections = HashMap<String, String>(256)

    private class TrieNode {
        val children = HashMap<Char, TrieNode>(4)
        var word: String? = null
        var freq: Int = 0
    }
    private val trieRoot = TrieNode()

    init {
        loadVocabulary()
        loadPhrases()
        loadTypoCorrections()
    }

    private fun insertToTrie(word: String, freq: Int) {
        var curr = trieRoot
        for (i in 0 until word.length) {
            val ch = word[i]
            curr = curr.children.getOrPut(ch) { TrieNode() }
        }
        curr.word = word
        curr.freq = maxOf(curr.freq, freq)
    }

    private fun searchPrefixInTrie(prefix: String, limit: Int = 50): List<Pair<String, Int>> {
        var curr = trieRoot
        for (i in 0 until prefix.length) {
            curr = curr.children[prefix[i]] ?: return emptyList()
        }
        val results = ArrayList<Pair<String, Int>>(limit)
        collectFromTrie(curr, results, limit)
        return results
    }

    private fun collectFromTrie(node: TrieNode, results: MutableList<Pair<String, Int>>, limit: Int) {
        if (node.word != null) {
            results.add(node.word!! to node.freq)
            if (results.size >= limit) return
        }
        for (child in node.children.values) {
            collectFromTrie(child, results, limit)
            if (results.size >= limit) return
        }
    }

    fun candidates(
        rawPrefix: String,
        preceding: List<String>,
        max: Int = 3
    ): List<Candidate> {
        if (max <= 0) return emptyList()

        val prefix = rawPrefix.trim()
        val lowerPrefix = prefix.lowercase(Locale.ENGLISH)
        val isAllUpper = prefix.length > 1 && prefix.all { it.isUpperCase() }
        val isTitle = prefix.isNotEmpty() && prefix[0].isUpperCase() && (prefix.length == 1 || prefix.drop(1).all { it.isLowerCase() })

        val previous = preceding.lastOrNull()?.trim()?.lowercase(Locale.ENGLISH)
        val earlier = preceding.dropLast(1).lastOrNull()?.trim()?.lowercase(Locale.ENGLISH)

        val learnedWords = learning.words()
        val learnedNext = previous?.let { learning.followers(it) }.orEmpty()
        val learnedTri = if (earlier != null && previous != null) learning.trigramFollowers(earlier, previous) else emptyMap()

        val staticNext = previous?.let { phraseBigrams[it] }.orEmpty()
        val staticTri = if (earlier != null && previous != null) phraseTrigrams["$earlier\t$previous"].orEmpty() else emptyList()

        val ranked = ArrayList<Candidate>(max * 2)
        val considered = HashSet<String>(64)

        fun applyCase(word: String): String {
            return when {
                isAllUpper -> word.uppercase(Locale.ENGLISH)
                isTitle -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                else -> word
            }
        }

        fun consider(candidateWord: String, frequency: Int, unigramWeight: Double, isCorrection: Boolean = false) {
            val lowerWord = candidateWord.lowercase(Locale.ENGLISH)
            if (!considered.add(lowerWord)) return

            val learnedCount = learnedWords[candidateWord] ?: learnedWords[lowerWord] ?: 0
            val learnedNextCount = learnedNext[candidateWord] ?: learnedNext[lowerWord] ?: 0
            val learnedTriCount = learnedTri[candidateWord] ?: learnedTri[lowerWord] ?: 0

            val staticNextCount = staticNext.firstOrNull { it.first.equals(lowerWord, ignoreCase = true) }?.second ?: 0
            val staticTriCount = staticTri.firstOrNull { it.first.equals(lowerWord, ignoreCase = true) }?.second ?: 0

            val score = unigramWeight * ln(frequency.coerceAtLeast(1) + 1.0) +
                    (if (learnedCount > 0) learnedCount * 25.0 + 50.0 else 0.0) +
                    (if (learnedNextCount > 0) learnedNextCount * 30.0 + 60.0 else 0.0) +
                    (if (learnedTriCount > 0) learnedTriCount * 40.0 + 80.0 else 0.0) +
                    ln(staticNextCount + 1.0) * 2.8 +
                    ln(staticTriCount + 1.0) * 3.8 +
                    if (isCorrection) 8.0 else 0.0

            val finalWord = applyCase(candidateWord)
            val cand = Candidate(finalWord, score, isCorrection)

            val index = ranked.indexOfFirst { cand.score > it.score }
            if (index < 0) {
                if (ranked.size < max * 2) ranked.add(cand)
            } else {
                ranked.add(index, cand)
                if (ranked.size > max * 2) ranked.removeAt(ranked.lastIndex)
            }
        }

        // 1. If prefix is empty -> Predict next words based on phrase context / bigrams / sentence starters
        if (prefix.isEmpty()) {
            if (previous != null) {
                // Learned trigrams & bigrams
                learnedTri.forEach { (word, count) ->
                    consider(word, count * 20, 1.5)
                }
                staticTri.forEach { (word, count) ->
                    consider(word, count, 1.4)
                }
                learnedNext.forEach { (word, count) ->
                    consider(word, count * 15, 1.2)
                }
                staticNext.forEach { (word, count) ->
                    consider(word, count, 1.0)
                }
            }

            if (ranked.isEmpty()) {
                // Default high frequency conversational words / sentence starters
                val starters = if (previous == null) {
                    listOf("I", "The", "How", "What", "Hello", "Thanks", "Good", "Can", "Please", "Are", "We", "You", "Where", "Let", "Have")
                } else {
                    listOf("and", "the", "to", "you", "a", "in", "it", "is", "for", "that", "on", "with", "my", "of", "be")
                }
                starters.forEach { word ->
                    consider(word, unigramIndex[word.lowercase(Locale.ENGLISH)] ?: 50, 1.0)
                }
            }

            return ranked.take(max)
        }

        // 2. Exact typo / contraction correction match (e.g., "teh" -> "the", "dont" -> "don't", "im" -> "I'm")
        val directCorrection = typoCorrections[lowerPrefix]
        if (directCorrection != null) {
            val formatted = if (directCorrection == "i" || directCorrection.startsWith("i'") || directCorrection == "i'm" || directCorrection == "i'll" || directCorrection == "i'd" || directCorrection == "i've") {
                directCorrection.replaceFirstChar { it.uppercase(Locale.ENGLISH) }
            } else {
                applyCase(directCorrection)
            }
            ranked.add(0, Candidate(formatted, 9999.0, isCorrection = true))
            considered.add(directCorrection.lowercase(Locale.ENGLISH))
        }

        // 3. User learned words matching prefix
        learnedWords.forEach { (word, count) ->
            if (word.startsWith(prefix, ignoreCase = true)) {
                consider(word, count * 35, 3.0)
            }
        }

        // 4. Context continuation matches matching prefix
        learnedTri.forEach { (word, count) ->
            if (word.startsWith(prefix, ignoreCase = true)) consider(word, count * 20, 2.5)
        }
        staticTri.forEach { (word, count) ->
            if (word.startsWith(prefix, ignoreCase = true)) consider(word, count, 2.2)
        }
        learnedNext.forEach { (word, count) ->
            if (word.startsWith(prefix, ignoreCase = true)) consider(word, count * 15, 2.0)
        }
        staticNext.forEach { (word, count) ->
            if (word.startsWith(prefix, ignoreCase = true)) consider(word, count, 1.8)
        }

        // 5. Dictionary prefix matches (fast Trie query)
        val trieMatches = searchPrefixInTrie(lowerPrefix, limit = 50)
        for ((word, freq) in trieMatches) {
            consider(word, freq, 2.5)
        }

        // 6. Fuzzy edit distance / Auto-correction if candidates are few
        if (ranked.size < max && lowerPrefix.length >= 3) {
            val fuzzyMatches = findFuzzyMatches(lowerPrefix)
            for ((word, freq, dist) in fuzzyMatches) {
                val penalty = when (dist) {
                    1 -> 0.85
                    else -> 0.65
                }
                val isTypoCorrection = dist == 1 && (unigramIndex[lowerPrefix] == null || (unigramIndex[lowerPrefix] ?: 0) < freq / 10)
                consider(word, (freq * penalty).toInt(), penalty, isTypoCorrection)
            }
        }

        return ranked.take(max)
    }

    fun learn(word: String, previous: String?, earlier: String? = null) {
        if (word.isBlank()) return
        learning.record(word, previous, earlier)
    }

    private fun findFuzzyMatches(input: String): List<Triple<String, Int, Int>> {
        val results = ArrayList<Triple<String, Int, Int>>(8)
        val inputLen = input.length
        val firstChar = input[0]

        // Candidate pool: only words starting with firstChar or adjacent keyboard keys
        val candidatesPool = ArrayList<Pair<String, Int>>(128)
        wordsByFirstChar[firstChar]?.let { candidatesPool.addAll(it) }
        val adjacent = getAdjacentChars(firstChar)
        for (adj in adjacent) {
            wordsByFirstChar[adj]?.let { candidatesPool.addAll(it) }
        }

        for (i in 0 until candidatesPool.size) {
            val (word, freq) = candidatesPool[i]
            if (abs(word.length - inputLen) > 2) continue

            val dist = levenshteinDistance(input, word, maxLimit = 2)
            if (dist in 1..2) {
                results.add(Triple(word, freq, dist))
                if (results.size >= 8) break
            }
        }
        return results.sortedBy { it.third * 1000 - it.second }
    }

    private fun getAdjacentChars(c: Char): List<Char> {
        return when (c) {
            'q' -> listOf('w', 'a', 's')
            'w' -> listOf('q', 'e', 'a', 's', 'd')
            'e' -> listOf('w', 'r', 's', 'd', 'f')
            'r' -> listOf('e', 't', 'd', 'f', 'g')
            't' -> listOf('r', 'y', 'f', 'g', 'h')
            'y' -> listOf('t', 'u', 'g', 'h', 'j')
            'u' -> listOf('y', 'i', 'h', 'j', 'k')
            'i' -> listOf('u', 'o', 'j', 'k', 'l')
            'o' -> listOf('i', 'p', 'k', 'l')
            'p' -> listOf('o', 'l')
            'a' -> listOf('q', 'w', 's', 'z')
            's' -> listOf('a', 'w', 'e', 'd', 'x', 'z')
            'd' -> listOf('s', 'e', 'r', 'f', 'c', 'x')
            'f' -> listOf('d', 'r', 't', 'g', 'v', 'c')
            'g' -> listOf('f', 't', 'y', 'h', 'b', 'v')
            'h' -> listOf('g', 'y', 'u', 'j', 'n', 'b')
            'j' -> listOf('h', 'u', 'i', 'k', 'm', 'n')
            'k' -> listOf('j', 'i', 'o', 'l', 'm')
            'l' -> listOf('k', 'o', 'p')
            'z' -> listOf('a', 's', 'x')
            'x' -> listOf('z', 's', 'd', 'c')
            'c' -> listOf('x', 'd', 'f', 'v')
            'v' -> listOf('c', 'f', 'g', 'b')
            'b' -> listOf('v', 'g', 'h', 'n')
            'n' -> listOf('b', 'h', 'j', 'm')
            'm' -> listOf('n', 'j', 'k')
            else -> emptyList()
        }
    }

    private fun levenshteinDistance(s1: String, s2: String, maxLimit: Int): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (abs(len1 - len2) > maxLimit) return maxLimit + 1

        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            var minInRow = curr[0]
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
                minInRow = min(minInRow, curr[j])
            }
            if (minInRow > maxLimit) return maxLimit + 1
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[len2]
    }

    private fun loadVocabulary() {
        // High-frequency curated English dictionary with frequency ranks (100 = most frequent, 10 = common)
        val rawVocab = listOf(
            // Top 100 English words
            "the" to 1000, "be" to 950, "to" to 940, "of" to 930, "and" to 920, "a" to 910, "in" to 900,
            "that" to 890, "have" to 880, "i" to 870, "it" to 860, "for" to 850, "not" to 840, "on" to 830,
            "with" to 820, "he" to 810, "as" to 800, "you" to 790, "do" to 780, "at" to 770, "this" to 760,
            "but" to 750, "his" to 740, "by" to 730, "from" to 720, "they" to 710, "we" to 700, "say" to 690,
            "her" to 680, "she" to 670, "or" to 660, "an" to 650, "will" to 640, "my" to 630, "one" to 620,
            "all" to 610, "would" to 600, "there" to 590, "their" to 580, "what" to 570, "so" to 560, "up" to 550,
            "out" to 540, "if" to 530, "about" to 520, "who" to 510, "get" to 500, "which" to 490, "go" to 480,
            "me" to 470, "when" to 460, "make" to 450, "can" to 440, "like" to 430, "time" to 420, "no" to 410,
            "just" to 400, "him" to 390, "know" to 380, "take" to 370, "people" to 360, "into" to 350, "year" to 340,
            "your" to 330, "good" to 320, "some" to 310, "could" to 300, "them" to 290, "see" to 280, "other" to 270,
            "than" to 260, "then" to 250, "now" to 240, "look" to 230, "only" to 220, "come" to 210, "its" to 200,
            "over" to 190, "think" to 180, "also" to 170, "back" to 160, "after" to 150, "use" to 140, "two" to 130,
            "how" to 120, "our" to 110, "work" to 100, "first" to 95, "well" to 90, "way" to 85, "even" to 80,
            "new" to 75, "want" to 70, "because" to 65, "any" to 60, "these" to 55, "give" to 50, "day" to 45,
            "most" to 40, "us" to 35,

            // Common Contractions & Pronouns
            "i'm" to 700, "i'll" to 650, "i've" to 600, "i'd" to 550, "don't" to 680, "can't" to 650, "won't" to 600,
            "didn't" to 580, "doesn't" to 540, "isn't" to 520, "aren't" to 500, "wasn't" to 480, "weren't" to 460,
            "haven't" to 450, "hasn't" to 440, "hadn't" to 430, "couldn't" to 450, "shouldn't" to 440, "wouldn't" to 450,
            "that's" to 620, "what's" to 600, "it's" to 650, "there's" to 550, "here's" to 500, "let's" to 560,
            "you're" to 620, "they're" to 580, "we're" to 590, "you've" to 500, "we've" to 500, "they've" to 480,
            "you'll" to 520, "we'll" to 530, "they'll" to 490, "how's" to 480, "who's" to 450, "where's" to 470,

            // Conversational, Greetings & Polite Phrases
            "hello" to 600, "hi" to 620, "hey" to 610, "thanks" to 650, "thank" to 640, "please" to 630,
            "sorry" to 600, "welcome" to 550, "sure" to 540, "okay" to 580, "ok" to 620, "yeah" to 560,
            "yes" to 590, "fine" to 500, "cool" to 480, "great" to 540, "awesome" to 500, "amazing" to 480,
            "morning" to 520, "night" to 520, "afternoon" to 450, "evening" to 450, "bye" to 480, "tomorrow" to 510,
            "yesterday" to 480, "today" to 550, "tonight" to 490, "soon" to 500, "later" to 520, "always" to 480,
            "never" to 470, "already" to 460, "again" to 480, "together" to 440, "everyone" to 460, "anyone" to 450,
            "everything" to 480, "nothing" to 460, "something" to 490, "someone" to 450, "somewhere" to 420,

            // Communication, Work & Tech
            "message" to 460, "call" to 520, "send" to 510, "text" to 480, "email" to 490, "link" to 470,
            "code" to 450, "file" to 460, "image" to 440, "photo" to 450, "video" to 460, "app" to 480,
            "phone" to 490, "number" to 480, "meeting" to 470, "office" to 460, "home" to 520, "work" to 530,
            "school" to 450, "class" to 440, "job" to 460, "money" to 450, "bank" to 430, "card" to 440,
            "order" to 450, "check" to 480, "help" to 500, "start" to 470, "stop" to 450, "wait" to 480,
            "ready" to 490, "free" to 480, "busy" to 470, "done" to 510, "open" to 460, "close" to 440,
            "update" to 450, "online" to 460, "offline" to 420, "account" to 450, "password" to 430,

            // Sri Lankan English / Local Slang & Culture
            "machan" to 500, "macho" to 400, "ado" to 450, "ela" to 480, "kiri" to 420, "patta" to 450,
            "bro" to 550, "dude" to 450, "man" to 500, "buddy" to 420, "sir" to 480, "madam" to 420,
            "aiyya" to 400, "akka" to 400, "mallie" to 400, "nangi" to 400, "aunty" to 420, "uncle" to 420,
            "colombo" to 420, "kandy" to 400, "galle" to 380, "lanka" to 450, "sri" to 450, "ceylon" to 360,
            "kohomada" to 450, "mokada" to 420, "waren" to 400, "yako" to 400, "shape" to 450, "scene" to 440,
            "pissuda" to 420, "hari" to 460, "ow" to 440, "naa" to 420, "ah" to 450, "ane" to 440,

            // Action Verbs
            "know" to 500, "think" to 490, "tell" to 480, "ask" to 470, "need" to 520, "feel" to 460,
            "find" to 480, "give" to 490, "leave" to 460, "put" to 470, "mean" to 460, "keep" to 470,
            "let" to 500, "begin" to 440, "seem" to 450, "help" to 500, "talk" to 510, "turn" to 450,
            "start" to 480, "show" to 470, "hear" to 460, "play" to 450, "run" to 460, "move" to 450,
            "live" to 460, "believe" to 480, "bring" to 460, "happen" to 470, "write" to 470, "provide" to 440,
            "sit" to 440, "stand" to 440, "lose" to 440, "pay" to 460, "meet" to 490, "include" to 430,
            "continue" to 440, "set" to 460, "learn" to 450, "change" to 470, "lead" to 430, "understand" to 480,
            "watch" to 460, "follow" to 450, "stop" to 470, "create" to 450, "speak" to 470, "read" to 470,
            "allow" to 440, "add" to 460, "spend" to 450, "grow" to 430, "open" to 460, "walk" to 450,
            "win" to 450, "offer" to 430, "remember" to 480, "love" to 540, "consider" to 430, "appear" to 420,
            "buy" to 470, "wait" to 490, "serve" to 420, "die" to 420, "send" to 510, "expect" to 440,
            "build" to 440, "stay" to 470, "fall" to 430, "cut" to 440, "reach" to 430, "kill" to 400,
            "remain" to 420, "suggest" to 440, "raise" to 420, "pass" to 440, "sell" to 440, "require" to 430,
            "report" to 430, "decide" to 450, "pull" to 420, "drive" to 460, "break" to 440, "wear" to 430,
            "receive" to 470, "agree" to 450, "support" to 440, "hit" to 430, "produce" to 420, "eat" to 470,
            "cover" to 430, "catch" to 440, "draw" to 430, "choose" to 450, "listen" to 470, "hope" to 490,
            "wish" to 480, "try" to 500, "enjoy" to 470, "care" to 480, "join" to 460, "reach" to 440,

            // Adjectives & Adverbs
            "happy" to 520, "sad" to 430, "glad" to 460, "excited" to 470, "tired" to 480, "hungry" to 440,
            "busy" to 490, "free" to 500, "late" to 490, "early" to 470, "fast" to 470, "slow" to 440,
            "easy" to 480, "hard" to 470, "simple" to 460, "difficult" to 440, "important" to 470, "possible" to 460,
            "beautiful" to 480, "nice" to 520, "pretty" to 460, "cute" to 470, "sweet" to 460, "funny" to 460,
            "serious" to 440, "special" to 460, "perfect" to 480, "best" to 530, "better" to 500, "worst" to 430,
            "big" to 480, "small" to 480, "huge" to 450, "tiny" to 430, "long" to 470, "short" to 460,
            "high" to 460, "low" to 450, "deep" to 430, "hot" to 460, "cold" to 460, "warm" to 450,
            "cool" to 480, "clean" to 450, "dirty" to 430, "fresh" to 450, "safe" to 480, "dangerous" to 430,
            "really" to 540, "very" to 530, "too" to 520, "quite" to 470, "pretty" to 480, "almost" to 470,
            "maybe" to 500, "probably" to 480, "definitely" to 490, "absolutely" to 470, "certainly" to 460,
            "actually" to 510, "basically" to 470, "totally" to 470, "obviously" to 460, "seriously" to 470,
            "currently" to 460, "recently" to 460, "finally" to 470, "immediately" to 460, "directly" to 450,
            "quickly" to 470, "slowly" to 440, "carefully" to 450, "easily" to 460, "perfectlys" to 420,

            // Time & Quantities
            "minute" to 460, "hour" to 470, "day" to 520, "week" to 500, "month" to 480, "year" to 500,
            "moment" to 460, "second" to 460, "morning" to 520, "noon" to 430, "evening" to 460, "midnight" to 430,
            "monday" to 470, "tuesday" to 460, "wednesday" to 460, "thursday" to 460, "friday" to 480, "saturday" to 480, "sunday" to 480,
            "january" to 420, "february" to 420, "march" to 420, "april" to 420, "may" to 450, "june" to 420,
            "july" to 420, "august" to 420, "september" to 420, "october" to 420, "november" to 420, "december" to 430,
            "much" to 500, "many" to 490, "few" to 460, "little" to 470, "lot" to 500, "lots" to 470,
            "more" to 520, "less" to 470, "least" to 440, "enough" to 470, "half" to 460, "quarter" to 440,
            "full" to 460, "empty" to 440, "zero" to 430, "first" to 500, "second" to 480, "third" to 460,

            // Questions & Conjunctions
            "who" to 520, "whom" to 420, "whose" to 440, "what" to 580, "which" to 510, "where" to 540,
            "when" to 540, "why" to 530, "how" to 560, "whether" to 450, "while" to 470, "although" to 450,
            "though" to 460, "unless" to 450, "since" to 470, "until" to 470, "till" to 460, "before" to 480,
            "after" to 490, "during" to 460, "without" to 480, "within" to 460, "between" to 470, "among" to 440,
            "through" to 470, "against" to 460, "towards" to 450, "upon" to 440, "behind" to 450, "beyond" to 440
        )

        for ((word, freq) in rawVocab) {
            val lower = word.lowercase(Locale.ENGLISH)
            unigramIndex[lower] = freq
            val entry = lower to freq
            wordList.add(entry)
            insertToTrie(lower, freq)
            if (lower.isNotEmpty()) {
                wordsByFirstChar.getOrPut(lower[0]) { ArrayList(64) }.add(entry)
            }
        }
    }

    private fun loadPhrases() {
        fun addBigram(from: String, to: String, score: Int = 10) {
            phraseBigrams.getOrPut(from.lowercase(Locale.ENGLISH)) { mutableListOf() }
                .add(to to score)
        }

        fun addTrigram(w1: String, w2: String, to: String, score: Int = 15) {
            val key = "${w1.lowercase(Locale.ENGLISH)}\t${w2.lowercase(Locale.ENGLISH)}"
            phraseTrigrams.getOrPut(key) { mutableListOf() }
                .add(to to score)
        }

        // Common phrase bigrams
        addBigram("how", "are", 50); addBigram("how", "is", 45); addBigram("how", "to", 40); addBigram("how", "about", 35); addBigram("how", "was", 35)
        addBigram("how", "much", 35); addBigram("how", "many", 35); addBigram("how", "do", 35); addBigram("how", "can", 30)

        addBigram("good", "morning", 50); addBigram("good", "night", 50); addBigram("good", "evening", 45); addBigram("good", "afternoon", 45)
        addBigram("good", "luck", 40); addBigram("good", "job", 40); addBigram("good", "one", 35); addBigram("good", "idea", 35); addBigram("good", "to", 35)

        addBigram("thank", "you", 60); addBigram("thank", "god", 35)
        addBigram("thanks", "for", 50); addBigram("thanks", "a", 45); addBigram("thanks", "bro", 45); addBigram("thanks", "so", 40); addBigram("thanks", "machan", 40); addBigram("thanks", "again", 35)

        addBigram("see", "you", 55); addBigram("see", "later", 40); addBigram("see", "soon", 40); addBigram("see", "tomorrow", 35)
        addBigram("let", "me", 55); addBigram("let", "us", 45); addBigram("let", "you", 40)
        addBigram("let's", "go", 50); addBigram("let's", "do", 45); addBigram("let's", "meet", 45); addBigram("let's", "see", 40)

        addBigram("i", "am", 60); addBigram("i", "will", 55); addBigram("i", "have", 55); addBigram("i", "can", 50); addBigram("i", "want", 50); addBigram("i", "know", 50)
        addBigram("i", "think", 50); addBigram("i", "love", 50); addBigram("i", "need", 45); addBigram("i", "was", 45); addBigram("i", "don't", 55); addBigram("i", "got", 45)

        addBigram("i'm", "going", 50); addBigram("i'm", "at", 45); addBigram("i'm", "on", 45); addBigram("i'm", "in", 45); addBigram("i'm", "sorry", 45); addBigram("i'm", "ready", 45); addBigram("i'm", "fine", 40); addBigram("i'm", "busy", 40)
        addBigram("i'll", "be", 50); addBigram("i'll", "call", 50); addBigram("i'll", "let", 45); addBigram("i'll", "come", 45); addBigram("i'll", "send", 45); addBigram("i'll", "do", 40); addBigram("i'll", "try", 40)

        addBigram("take", "care", 55); addBigram("take", "it", 45); addBigram("take", "time", 40); addBigram("take", "your", 40)
        addBigram("nice", "to", 50); addBigram("nice", "day", 40); addBigram("nice", "one", 40)
        addBigram("no", "problem", 50); addBigram("no", "worries", 45); addBigram("no", "idea", 40); addBigram("no", "way", 40); addBigram("no", "need", 40)
        addBigram("on", "my", 50); addBigram("on", "the", 50); addBigram("on", "time", 45); addBigram("on", "it", 40)
        addBigram("at", "home", 45); addBigram("at", "work", 45); addBigram("at", "the", 50); addBigram("at", "office", 40); addBigram("at", "night", 40)
        addBigram("in", "the", 55); addBigram("in", "a", 45); addBigram("in", "touch", 40); addBigram("in", "fact", 40)
        addBigram("what", "is", 50); addBigram("what", "are", 45); addBigram("what", "about", 45); addBigram("what", "time", 45); addBigram("what", "do", 40); addBigram("what", "happened", 40)
        addBigram("where", "are", 50); addBigram("where", "is", 45); addBigram("where", "to", 40); addBigram("where", "can", 35)
        addBigram("when", "will", 45); addBigram("when", "are", 45); addBigram("when", "can", 40); addBigram("when", "is", 40)
        addBigram("why", "not", 50); addBigram("why", "are", 40); addBigram("why", "did", 40); addBigram("why", "is", 35)
        addBigram("can", "you", 55); addBigram("can", "i", 50); addBigram("can", "we", 45); addBigram("can", "be", 40)
        addBigram("could", "you", 50); addBigram("could", "be", 40); addBigram("could", "have", 35)
        addBigram("would", "you", 50); addBigram("would", "like", 45); addBigram("would", "be", 40)
        addBigram("are", "you", 55); addBigram("are", "there", 45); addBigram("are", "we", 40); addBigram("are", "they", 35)
        addBigram("have", "a", 55); addBigram("have", "to", 50); addBigram("have", "been", 45); addBigram("have", "you", 45); addBigram("have", "fun", 40)
        addBigram("please", "let", 50); addBigram("please", "call", 45); addBigram("please", "send", 45); addBigram("please", "help", 40); addBigram("please", "check", 40)
        addBigram("happy", "birthday", 55); addBigram("happy", "new", 50); addBigram("happy", "weekend", 40); addBigram("happy", "anniversary", 40)
        addBigram("call", "me", 50); addBigram("call", "you", 45); addBigram("call", "back", 45); addBigram("call", "later", 40)
        addBigram("talk", "to", 50); addBigram("talk", "later", 45); addBigram("talk", "soon", 40)
        addBigram("sounds", "good", 50); addBigram("sounds", "great", 45); addBigram("sounds", "like", 40)
        addBigram("looking", "forward", 50); addBigram("looking", "for", 45); addBigram("looking", "good", 40)

        // Sri Lankan collocations
        addBigram("machan", "kohomada", 50); addBigram("machan", "mokada", 45); addBigram("machan", "waren", 40); addBigram("machan", "ado", 40); addBigram("machan", "call", 40)
        addBigram("ela", "machan", 50); addBigram("ela", "kiri", 50); addBigram("ela", "bro", 45)
        addBigram("ado", "machan", 50); addBigram("ado", "mokada", 45); addBigram("ado", "kohomada", 40)

        // Common phrase trigrams
        addTrigram("how", "are", "you", 60); addTrigram("how", "are", "things", 40); addTrigram("how", "are", "we", 35)
        addTrigram("let", "me", "know", 60); addTrigram("let", "me", "see", 45); addTrigram("let", "me", "check", 45); addTrigram("let", "me", "call", 40)
        addTrigram("nice", "to", "meet", 55); addTrigram("nice", "to", "see", 50); addTrigram("nice", "to", "hear", 45)
        addTrigram("have", "a", "good", 55); addTrigram("have", "a", "great", 55); addTrigram("have", "a", "nice", 50); addTrigram("have", "a", "safe", 45); addTrigram("have", "a", "wonderful", 40)
        addTrigram("happy", "birthday", "to", 55); addTrigram("happy", "birthday", "bro", 50); addTrigram("happy", "birthday", "machan", 45)
        addTrigram("take", "care", "of", 50); addTrigram("take", "care", "bro", 50); addTrigram("take", "care", "machan", 45)
        addTrigram("no", "problem", "bro", 50); addTrigram("no", "problem", "at", 45); addTrigram("no", "problem", "machan", 45)
        addTrigram("on", "my", "way", 60); addTrigram("on", "my", "own", 45); addTrigram("on", "my", "mind", 40)
        addTrigram("see", "you", "later", 55); addTrigram("see", "you", "soon", 55); addTrigram("see", "you", "tomorrow", 50)
        addTrigram("talk", "to", "you", 55); addTrigram("talk", "to", "him", 40); addTrigram("talk", "to", "her", 40)
        addTrigram("i", "am", "going", 50); addTrigram("i", "am", "sorry", 50); addTrigram("i", "am", "ready", 45); addTrigram("i", "am", "at", 45)
        addTrigram("i", "will", "call", 50); addTrigram("i", "will", "be", 50); addTrigram("i", "will", "let", 45); addTrigram("i", "will", "come", 45); addTrigram("i", "will", "send", 45)
        addTrigram("i", "don't", "know", 60); addTrigram("i", "don't", "think", 50); addTrigram("i", "don't", "have", 45); addTrigram("i", "don't", "mind", 40)
        addTrigram("i", "want", "to", 55); addTrigram("i", "need", "to", 55); addTrigram("i", "love", "you", 60); addTrigram("i", "hope", "you", 50)
        addTrigram("can", "you", "please", 55); addTrigram("can", "you", "call", 50); addTrigram("can", "you", "send", 50); addTrigram("can", "you", "help", 45)
        addTrigram("could", "you", "please", 55); addTrigram("could", "you", "send", 45); addTrigram("could", "you", "check", 45)
        addTrigram("would", "you", "like", 55); addTrigram("would", "like", "to", 55)
        addTrigram("are", "you", "free", 50); addTrigram("are", "you", "ready", 50); addTrigram("are", "you", "sure", 50); addTrigram("are", "you", "there", 50); addTrigram("are", "you", "coming", 45)
        addTrigram("where", "are", "you", 60); addTrigram("where", "are", "we", 45)
        addTrigram("what", "are", "you", 55); addTrigram("what", "time", "is", 50); addTrigram("what", "time", "will", 45)
        addTrigram("looking", "forward", "to", 60)
        addTrigram("as", "soon", "as", 60)
        addTrigram("by", "the", "way", 60)
        addTrigram("thank", "you", "so", 55); addTrigram("thank", "you", "very", 50); addTrigram("thank", "you", "bro", 50)
    }

    private fun loadTypoCorrections() {
        val map = listOf(
            // Contractions without apostrophes
            "im" to "I'm", "dont" to "don't", "cant" to "can't", "wont" to "won't", "didnt" to "didn't",
            "doesnt" to "doesn't", "isnt" to "isn't", "arent" to "aren't", "wasnt" to "wasn't", "werent" to "weren't",
            "havent" to "haven't", "hasnt" to "hasn't", "hadnt" to "hadn't", "couldnt" to "couldn't", "shouldnt" to "shouldn't",
            "wouldnt" to "wouldn't", "thats" to "that's", "whats" to "what's", "hows" to "how's", "wheres" to "where's",
            "theres" to "there's", "lets" to "let's", "youre" to "you're", "theyre" to "they're", "weve" to "we've",
            "youve" to "you've", "theyve" to "they've", "ill" to "I'll", "youll" to "you'll", "theyll" to "they'll",
            "id" to "I'd", "ive" to "I've",

            // Common Misspellings & Typo Swaps
            "teh" to "the", "taht" to "that", "waht" to "what", "wierd" to "weird", "beleive" to "believe",
            "seperate" to "separate", "definately" to "definitely", "recieved" to "received", "recieve" to "receive",
            "occured" to "occurred", "untill" to "until", "alot" to "a lot", "tommorrow" to "tomorrow",
            "tommorow" to "tomorrow", "thier" to "their", "guarentee" to "guarantee", "accomodate" to "accommodate",
            "acheive" to "achieve", "calender" to "calendar", "concious" to "conscious", "experiance" to "experience",
            "foriegn" to "foreign", "goverment" to "government", "grammer" to "grammar", "happended" to "happened",
            "interupt" to "interrupt", "mispell" to "misspell", "neccessary" to "necessary", "noticable" to "noticeable",
            "occurence" to "occurrence", "peice" to "piece", "posession" to "possession", "privilege" to "privilege",
            "reccomend" to "recommend", "remeber" to "remember", "suprise" to "surprise", "truely" to "truly",
            "unfortunatly" to "unfortunately", "writting" to "writing", "yu" to "you", "u" to "you",
            "r" to "are", "ur" to "your", "pls" to "please", "plz" to "please", "thx" to "thanks",
            "tks" to "thanks", "ty" to "thank you", "np" to "no problem", "idk" to "I don't know",
            "imo" to "in my opinion", "btw" to "by the way", "omw" to "on my way", "tbh" to "to be honest",
            "rn" to "right now", "gm" to "good morning", "gn" to "good night", "hbd" to "happy birthday",
            "bday" to "birthday", "tmrw" to "tomorrow", "yday" to "yesterday", "pic" to "picture",
            "pics" to "pictures", "msg" to "message", "broo" to "bro", "machann" to "machan"
        )
        for ((typo, fix) in map) {
            typoCorrections[typo.lowercase(Locale.ENGLISH)] = fix
        }
    }
}
