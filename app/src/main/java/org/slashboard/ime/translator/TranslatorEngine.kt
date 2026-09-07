package org.slashboard.ime.translator

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.slashboard.ime.engine.SinhalaEngine
import org.slashboard.ime.engine.InputMode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, bi-directional Translation Engine for Sinhala ⇄ English.
 * Provides both real-time online API translation and an offline dictionary fallback.
 */
object TranslatorEngine {

    private val cache = ConcurrentHashMap<String, String>()

    // Persistent Dynamic Cache
    private val persistentCache = ConcurrentHashMap<String, String>()
    private var contextRef: Context? = null

    fun init(context: Context) {
        contextRef = context.applicationContext
        loadPersistentCache(context)
    }

    private fun loadPersistentCache(context: Context) {
        runCatching {
            val file = java.io.File(context.filesDir, "translator_cache.json")
            if (file.exists()) {
                val text = file.readText()
                val json = JSONObject(text)
                json.keys().forEach { key ->
                    persistentCache[key] = json.getString(key)
                }
            }
        }
    }

    private fun saveToPersistentCache(key: String, value: String) {
        persistentCache[key] = value
        contextRef?.let { ctx ->
            runCatching {
                val file = java.io.File(ctx.filesDir, "translator_cache.json")
                val json = JSONObject(persistentCache as Map<*, *>)
                file.writeText(json.toString())
            }
        }
    }

    /**
     * Common Phrases and Rich Dictionary for Offline Fallback & Instant Lookups
     */
    private val sinhalaToEnglishDictionary = mapOf(
        // Greetings & Etiquette
        "ආයුබෝවන්" to "Hello",
        "හායි" to "Hi",
        "හෙලෝ" to "Hello",
        "සුබ උදෑසනක්" to "Good morning",
        "සුබ රාත්‍රියක්" to "Good night",
        "සුබ සන්ධ්‍යාවක්" to "Good evening",
        "සුබ දවසක්" to "Have a nice day",
        "ස්තූතියි" to "Thank you",
        "බොහොම ස්තූතියි" to "Thank you very much",
        "ගොඩක් ස්තූතියි" to "Thanks a lot",
        "කරුණාකර" to "Please",
        "සමාවෙන්න" to "Sorry",
        "සුබ පැතුම්" to "Congratulations",
        "සුබ උපන්දිනයක්" to "Happy Birthday",
        "සුබ අලුත් අවුරුද්දක්" to "Happy New Year",

        // Questions & Basics
        "කොහොමද" to "How are you?",
        "ඔයාට කොහොමද" to "How are you?",
        "ඔබට කොහොමද" to "How are you?",
        "මොකද වෙන්නේ" to "What's up?",
        "ඔයාගේ නම මොකක්ද" to "What is your name?",
        "ඔබේ නම කුමක්ද" to "What is your name?",
        "මගේ නම" to "My name is",
        "මොකක්ද" to "What?",
        "කොහෙද" to "Where?",
        "කවද්ද" to "When?",
        "ඇයි" to "Why?",
        "කවුද" to "Who?",
        "කොච්චරද" to "How much?",
        "මිල කීයද" to "How much is the price?",
        "ඔව්" to "Yes",
        "නැහැ" to "No",
        "නෑ" to "No",
        "හරි" to "Okay",
        "හරිම ලස්සනයි" to "Very beautiful",
        "එපා" to "No / Don't want",
        "පුළුවන්" to "Can / Possible",
        "බැහැ" to "Cannot",
        "දන්නේ නැහැ" to "I don't know",
        "දන්නේ නෑ" to "I don't know",
        "තේරෙන්නේ නැහැ" to "I don't understand",
        "මට තේරෙන්නේ නෑ" to "I don't understand",
        "උදව් කරන්න" to "Help me",
        "මට උදව්වක් ඕන" to "I need help",

        // Common Verbs & Actions
        "යනවා" to "Going",
        "යන්න" to "Go",
        "එනවා" to "Coming",
        "එන්න" to "Come",
        "කනවා" to "Eating",
        "කන්න" to "Eat",
        "බොනවා" to "Drinking",
        "බොන්න" to "Drink",
        "නිදාගන්නවා" to "Sleeping",
        "නිදාගන්න" to "Sleep",
        "වැඩ කරනවා" to "Working",
        "වැඩ" to "Work",
        "බලනවා" to "Looking",
        "බලන්න" to "Look / Watch",
        "ලියනවා" to "Writing",
        "ලියන්න" to "Write",
        "කියවනවා" to "Reading",
        "කියවන්න" to "Read",
        "කතා කරනවා" to "Speaking",
        "කතා කරන්න" to "Speak",
        "ගන්නවා" to "Taking / Buying",
        "ගන්න" to "Take",
        "දෙනවා" to "Giving",
        "දෙන්න" to "Give",
        "හිතනවා" to "Thinking",
        "දන්නවා" to "Knowing",

        // Pronouns
        "මම" to "I",
        "ඔයා" to "You",
        "ඔබ" to "You",
        "අපි" to "We",
        "එයා" to "He / She",
        "ඔවුන්" to "They",
        "මේක" to "This",
        "ඒක" to "That",

        // Places & Nouns
        "ගෙදර" to "Home",
        "රට" to "Country",
        "නගරය" to "City",
        "පාසල" to "School",
        "කාර්යාලය" to "Office",
        "කඩේ" to "Shop",
        "රෝහල" to "Hospital",
        "කෑම" to "Food",
        "වතුර" to "Water",
        "තේ" to "Tea",
        "බත්" to "Rice",
        "සල්ලි" to "Money",
        "වේලාව" to "Time",
        "අද" to "Today",
        "හෙට" to "Tomorrow",
        "ඊයේ" to "Yesterday",
        "දැන්" to "Now",
        "පස්සේ" to "Later",
        "උදේ" to "Morning",
        "හවස" to "Evening",
        "රෑ" to "Night",

        // Feelings & Expressions
        "ලස්සනයි" to "Beautiful",
        "හොඳයි" to "Good",
        "නරකයි" to "Bad",
        "සතුටුයි" to "Happy",
        "දුකයි" to "Sad",
        "මහන්සියි" to "Tired",
        "බඩගිනියි" to "Hungry",
        "තිබහයි" to "Thirsty",
        "ආදරෙයි" to "Love",
        "මම ඔයාට ආදරෙයි" to "I love you",
        "මම ශ්‍රී ලාංකිකයෙක්" to "I am a Sri Lankan"
    )

    private val englishToSinhalaDictionary = mapOf(
        // Greetings
        "hello" to "ආයුබෝවන්",
        "hi" to "හායි",
        "welcome" to "සාදරයෙන් පිළිගනිමු",
        "good morning" to "සුබ උදෑසනක්",
        "good night" to "සුබ රාත්‍රියක්",
        "good evening" to "සුබ සන්ධ්‍යාවක්",
        "good afternoon" to "සුබ දහවලක්",
        "have a nice day" to "සුබ දවසක්",
        "thank you" to "ස්තූතියි",
        "thanks" to "ස්තූතියි",
        "thank you very much" to "බොහොම ස්තූතියි",
        "please" to "කරුණාකර",
        "sorry" to "සමාවෙන්න",
        "excuse me" to "සමාවෙන්න",
        "congratulations" to "සුබ පැතුම්",
        "happy birthday" to "සුබ උපන්දිනයක්",
        "happy new year" to "සුබ අලුත් අවුරුද්දක් වේවා",

        // Questions & Basics
        "how are you" to "ඔයාට කොහොමද?",
        "how are you?" to "ඔයාට කොහොමද?",
        "what is your name" to "ඔයාගේ නම මොකක්ද?",
        "what is your name?" to "ඔයාගේ නම මොකක්ද?",
        "my name is" to "මගේ නම",
        "what" to "මොකක්ද",
        "where" to "කොහෙද",
        "when" to "කවද්ද",
        "why" to "ඇයි",
        "who" to "කවුද",
        "how" to "කොහොමද",
        "how much" to "කොච්චරද",
        "how much is this" to "මේක කීයද?",
        "yes" to "ඔව්",
        "no" to "නැහැ",
        "okay" to "හරි",
        "ok" to "හරි",
        "fine" to "හොඳින්",
        "i am fine" to "මම හොඳින් ඉන්නවා",
        "i don't know" to "මම දන්නේ නැහැ",
        "i don't understand" to "මට තේරෙන්නේ නැහැ",
        "help" to "උදව් කරන්න",
        "help me" to "මට උදව් කරන්න",

        // Pronouns
        "i" to "මම",
        "you" to "ඔයා",
        "we" to "අපි",
        "he" to "ඔහු",
        "she" to "ඇය",
        "they" to "ඔවුන්",
        "this" to "මේක",
        "that" to "ඒක",

        // Verbs & Common Phrases
        "go" to "යන්න",
        "come" to "එන්න",
        "eat" to "කන්න",
        "drink" to "බොන්න",
        "sleep" to "නිදාගන්න",
        "work" to "වැඩ කරන්න",
        "speak" to "කතා කරන්න",
        "write" to "ලියන්න",
        "read" to "කියවන්න",
        "buy" to "මිලදී ගන්න",
        "give" to "දෙන්න",
        "take" to "ගන්න",
        "see" to "බලන්න",

        // Nouns & Time
        "home" to "ගෙදර",
        "house" to "නිවස",
        "school" to "පාසල",
        "office" to "කාර්යාලය",
        "water" to "වතුර",
        "food" to "කෑම",
        "tea" to "තේ",
        "money" to "සල්ලි",
        "time" to "වේලාව",
        "today" to "අද",
        "tomorrow" to "හෙට",
        "yesterday" to "ඊයේ",
        "now" to "දැන්",
        "later" to "පසුව",
        "morning" to "උදෑසන",
        "night" to "රාත්‍රිය",

        // Adjectives & Feelings
        "good" to "හොඳ",
        "bad" to "නරක",
        "beautiful" to "ලස්සන",
        "happy" to "සතුටුයි",
        "sad" to "දුකයි",
        "love" to "ආදරය",
        "i love you" to "මම ඔයාට ආදරෙයි"
    )

    /**
     * Translates text between Sinhala ("si") and English ("en").
     * @param text The input query string
     * @param sourceLang Source language code ("si" or "en" or "auto")
     * @param targetLang Target language code ("si" or "en")
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "si",
        targetLang: String = "en"
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext Result.success("")

        val actualSource = if (sourceLang == "auto") {
            if (isSinhala(trimmed)) "si" else "en"
        } else sourceLang

        val cacheKey = "$actualSource->$targetLang:$trimmed"
        cache[cacheKey]?.let { return@withContext Result.success(it) }
        persistentCache[cacheKey]?.let { 
            cache[cacheKey] = it
            return@withContext Result.success(it) 
        }

        // 1. Try exact dictionary/phrase match
        val dictResult = lookupDictionary(trimmed, actualSource, targetLang)
        if (dictResult != null) {
            cache[cacheKey] = dictResult
            return@withContext Result.success(dictResult)
        }

        // 2. Try online translation APIs (Google Translate endpoint 1 & 2)
        try {
            val onlineResult = fetchGoogleTranslate(trimmed, actualSource, targetLang)
            if (!onlineResult.isNullOrBlank()) {
                cache[cacheKey] = onlineResult
                saveToPersistentCache(cacheKey, onlineResult)
                return@withContext Result.success(onlineResult)
            }
        } catch (_: Exception) {
        }

        // 3. Try secondary online API (MyMemory)
        try {
            val myMemoryResult = fetchMyMemory(trimmed, actualSource, targetLang)
            if (!myMemoryResult.isNullOrBlank()) {
                cache[cacheKey] = myMemoryResult
                saveToPersistentCache(cacheKey, myMemoryResult)
                return@withContext Result.success(myMemoryResult)
            }
        } catch (_: Exception) {
        }

        // 4. Word-by-word fallback if multiple words
        val wordByWord = translateWordByWord(trimmed, actualSource, targetLang)
        if (wordByWord != null) {
            cache[cacheKey] = wordByWord
            return@withContext Result.success(wordByWord)
        }

        // 5. If translating Singlish to English, convert to Sinhala first then re-translate
        if (actualSource == "si" && !isSinhala(trimmed)) {
            val sinhalaText = SinhalaEngine.transliterate(trimmed, InputMode.SMART_PHONETIC)
            val subResult = translate(sinhalaText, "si", targetLang)
            if (subResult.isSuccess) {
                return@withContext subResult
            }
        }

        // 6. Safe Transliteration fallback: Never fail or show unavailable
        val safeFallback = if (actualSource == "si" && targetLang == "en") {
            // Romanize / Singlish representation
            SinhalaEngine.transliterate(trimmed, InputMode.SMART_PHONETIC)
        } else if (actualSource == "en" && targetLang == "si") {
            SinhalaEngine.transliterate(trimmed, InputMode.SMART_PHONETIC)
        } else {
            trimmed
        }
        cache[cacheKey] = safeFallback
        Result.success(safeFallback)
    }

    private fun lookupDictionary(text: String, sourceLang: String, targetLang: String): String? {
        val normalized = text.lowercase().trim().removeSuffix(".").removeSuffix("?").removeSuffix("!")
        if (sourceLang == "si" && targetLang == "en") {
            sinhalaToEnglishDictionary[text]?.let { return it }
            sinhalaToEnglishDictionary[normalized]?.let { return it }
        } else if (sourceLang == "en" && targetLang == "si") {
            englishToSinhalaDictionary[normalized]?.let { return it }
            englishToSinhalaDictionary[text]?.let { return it }
        }
        return null
    }

    private fun translateWordByWord(text: String, sourceLang: String, targetLang: String): String? {
        val words = text.split(Regex("\\s+"))
        if (words.size <= 1) return null

        val translatedWords = words.map { word ->
            val clean = word.trim().replace(Regex("[^\\p{L}\\p{Nd}]"), "")
            lookupDictionary(clean, sourceLang, targetLang) ?: word
        }
        val countMatched = translatedWords.zip(words).count { it.first != it.second }
        return if (countMatched > 0) translatedWords.joinToString(" ") else null
    }

    private fun fetchGoogleTranslate(text: String, sl: String, tl: String): String? {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        
        // Endpoint 1: Single client=gtx
        try {
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encodedText"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val response = reader.readText()
                reader.close()

                val jsonArray = JSONArray(response)
                val sentences = jsonArray.optJSONArray(0)
                if (sentences != null) {
                    val sb = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val sentence = sentences.optJSONArray(i)
                        if (sentence != null && sentence.length() > 0) {
                            sb.append(sentence.optString(0))
                        }
                    }
                    val res = sb.toString().trim()
                    if (res.isNotBlank()) return res
                }
            }
        } catch (_: Exception) {}

        // Endpoint 2: Dict chrome-ex fallback
        try {
            val urlStr = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=$sl&tl=$tl&q=$encodedText"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val response = reader.readText()
                reader.close()

                val json = JSONArray(response)
                if (json.length() > 0) {
                    val first = json.opt(0)
                    if (first is JSONArray && first.length() > 0) {
                        return first.optString(0).trim()
                    } else if (first is String && first.isNotBlank()) {
                        return first.trim()
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun fetchMyMemory(text: String, sl: String, tl: String): String? {
        val pair = "$sl|$tl"
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val urlStr = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$pair"
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 4000
        conn.readTimeout = 4000

        return try {
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val responseData = json.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText")
                if (!translated.isNullOrBlank() && !translated.startsWith("MYMEMORY WARNING")) {
                    translated
                } else null
            } else null
        } finally {
            conn.disconnect()
        }
    }

    fun isSinhala(text: String): Boolean {
        return text.any { it in '\u0D80'..'\u0DFF' }
    }

    /**
     * Common phrase category for interactive quick translator learning
     */
    data class PhraseItem(val sinhala: String, val english: String, val category: String)

    val samplePhrases = listOf(
        PhraseItem("ආයුබෝවන්", "Hello / Welcome", "Greetings"),
        PhraseItem("සුබ උදෑසනක්", "Good morning", "Greetings"),
        PhraseItem("සුබ රාත්‍රියක්", "Good night", "Greetings"),
        PhraseItem("ස්තූතියි", "Thank you", "Greetings"),
        PhraseItem("සමාවෙන්න", "Excuse me / Sorry", "Greetings"),
        PhraseItem("ඔයාට කොහොමද?", "How are you?", "Conversation"),
        PhraseItem("මම හොඳින් ඉන්නවා", "I am doing well", "Conversation"),
        PhraseItem("ඔයාගේ නම මොකක්ද?", "What is your name?", "Conversation"),
        PhraseItem("මගේ නම ...", "My name is ...", "Conversation"),
        PhraseItem("මේක කීයද?", "How much is this?", "Shopping"),
        PhraseItem("මට උදව් කරන්න පුළුවන්ද?", "Can you help me?", "Help"),
        PhraseItem("මට තේරෙන්නේ නැහැ", "I do not understand", "Help"),
        PhraseItem("රෝහල කොහෙද තියෙන්නේ?", "Where is the hospital?", "Travel"),
        PhraseItem("ස්තූතියි, ආයෙත් හමුවෙමු", "Thank you, see you again", "Greetings")
    )
}
