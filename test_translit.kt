import org.slashboard.ime.engine.*

fun main() {
    println("nAA -> " + SinhalaEngine.transliterate("nAA", InputMode.SMART_PHONETIC))
    println("nAa -> " + SinhalaEngine.transliterate("nAa", InputMode.SMART_PHONETIC))
    println("nA -> " + SinhalaEngine.transliterate("nA", InputMode.SMART_PHONETIC))
    println("x -> " + SinhalaEngine.transliterate("x", InputMode.SMART_PHONETIC))
    println("nx -> " + SinhalaEngine.transliterate("nx", InputMode.SMART_PHONETIC))
}
