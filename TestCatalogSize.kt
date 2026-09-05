import org.json.JSONObject
import java.io.File

fun main() {
    val text = File("app/src/main/res/raw/sinhala_emoji_index.json").readText()
    val index = mutableMapOf<String, MutableSet<String>>()
    val catalog = linkedSetOf<String>()
    
    val root = JSONObject(text)
    root.keys().forEach { key ->
        val value = root.get(key)
        when (value) {
            is String -> index.getOrPut(key.lowercase()) { linkedSetOf() }.add(value).also { catalog.add(value) }
            is org.json.JSONArray -> repeat(value.length()) { i -> value.getString(i).let { emoji -> index.getOrPut(key.lowercase()) { linkedSetOf() }.add(emoji); catalog.add(emoji) } }
        }
    }
    
    val buckets = linkedMapOf(
        "Smileys" to Pair("😀", mutableListOf<String>()), "People" to Pair("👋", mutableListOf()),
        "Nature" to Pair("🐻", mutableListOf()), "Food" to Pair("🍜", mutableListOf()),
        "Activities" to Pair("⚽", mutableListOf()), "Travel" to Pair("🚗", mutableListOf()),
        "Objects" to Pair("💡", mutableListOf()), "Symbols" to Pair("❤️", mutableListOf())
    )
    
    fun categoryFor(emoji: String): String {
        val cp = emoji.codePointAt(0)
        return when {
            cp in 0x1F600..0x1F64F || cp in 0x1F910..0x1F92F || cp in 0x1F970..0x1F97F -> "Smileys"
            cp in 0x1F466..0x1F487 || cp in 0x1F590..0x1F596 || cp in 0x1F645..0x1F64F || cp in 0x1F9B0..0x1F9DD -> "People"
            cp in 0x1F32D..0x1F37F || cp in 0x1F950..0x1F96F -> "Food"
            cp in 0x1F3A0..0x1F3FF -> "Activities"
            cp in 0x1F680..0x1F6FF -> "Travel"
            cp in 0x1F400..0x1F43E || cp in 0x1F980..0x1F9AE || cp in 0x1F300..0x1F32C -> "Nature"
            cp in 0x1F4A0..0x1F5FF -> "Objects"
            else -> "Symbols"
        }
    }
    
    catalog.forEach { emoji -> buckets.getValue(categoryFor(emoji)).second.add(emoji) }
    
    buckets.forEach { (name, pair) ->
        println("$name: ${pair.second.distinct().size}")
    }
}
