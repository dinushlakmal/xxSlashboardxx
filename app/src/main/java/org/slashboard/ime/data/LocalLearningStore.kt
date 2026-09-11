package org.slashboard.ime.data

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class LocalLearningStore(context: Context) {
    private val prefs = context.getSharedPreferences("slashboard_learning", Context.MODE_PRIVATE)
    private val cache = ConcurrentHashMap<String, MutableMap<String, Int>>()
    private val diskExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "slashboard-learning-store").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    }

    fun record(word: String, previous: String?, earlier: String? = null) {
        if (word.isBlank() || word.length < 2) return
        val wClean = word.trim()
        val words = getMap("words")
        val wordsSnapshot = synchronized(words) {
            words[wClean] = (words[wClean] ?: 0) + 1
            trim(words, 10000)
            HashMap(words)
        }
        writeAsync("words", wordsSnapshot)

        if (!previous.isNullOrBlank()) {
            val pClean = previous.trim()
            val bigramKey = "bigram:$pClean"
            val followers = getMap(bigramKey)
            val followersSnapshot = synchronized(followers) {
                followers[wClean] = (followers[wClean] ?: 0) + 1
                trim(followers, 256)
                HashMap(followers)
            }
            writeAsync(bigramKey, followersSnapshot)

            if (!earlier.isNullOrBlank()) {
                val eClean = earlier.trim()
                val trigramKey = "trigram:$eClean\t$pClean"
                val triFollowers = getMap(trigramKey)
                val triFollowersSnapshot = synchronized(triFollowers) {
                    triFollowers[wClean] = (triFollowers[wClean] ?: 0) + 1
                    trim(triFollowers, 128)
                    HashMap(triFollowers)
                }
                writeAsync(trigramKey, triFollowersSnapshot)
            }
        }
    }

    fun words(): Map<String, Int> = getMap("words").toMap()

    fun followers(previous: String?): Map<String, Int> {
        if (previous.isNullOrBlank()) return emptyMap()
        return getMap("bigram:${previous.trim()}").toMap()
    }

    fun trigramFollowers(earlier: String?, previous: String?): Map<String, Int> {
        if (earlier.isNullOrBlank() || previous.isNullOrBlank()) return emptyMap()
        return getMap("trigram:${earlier.trim()}\t${previous.trim()}").toMap()
    }

    fun clear() {
        cache.clear()
        diskExecutor.execute {
            prefs.edit().clear().apply()
        }
    }

    private fun getMap(key: String): MutableMap<String, Int> {
        return cache.computeIfAbsent(key) {
            readFromPrefs(key)
        }
    }

    private fun readFromPrefs(key: String): MutableMap<String, Int> {
        val jsonStr = prefs.getString(key, null) ?: return ConcurrentHashMap<String, Int>()
        val json = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return ConcurrentHashMap<String, Int>()
        val map = ConcurrentHashMap<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = json.optInt(k, 1)
        }
        return map
    }

    private fun writeAsync(key: String, map: Map<String, Int>) {
        diskExecutor.execute {
            runCatching {
                val json = JSONObject(map)
                prefs.edit().putString(key, json.toString()).apply()
            }
        }
    }

    private fun trim(map: MutableMap<String, Int>, max: Int) {
        if (map.size > max) {
            val toRemove = map.entries.sortedBy { it.value }.take(map.size - max).map { it.key }
            toRemove.forEach { map.remove(it) }
        }
    }
}


