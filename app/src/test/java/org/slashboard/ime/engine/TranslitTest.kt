package org.slashboard.ime.engine

import org.junit.Test
import org.junit.Assert.*

class TranslitTest {
    @Test
    fun testVowels() {
        assertEquals("අ", SinhalaEngine.transliterate("a", InputMode.SMART_PHONETIC))
        assertEquals("ආ", SinhalaEngine.transliterate("aa", InputMode.SMART_PHONETIC))
        assertEquals("ඇ", SinhalaEngine.transliterate("A", InputMode.SMART_PHONETIC))
        assertEquals("ඇ", SinhalaEngine.transliterate("ae", InputMode.SMART_PHONETIC))
        assertEquals("ඈ", SinhalaEngine.transliterate("Aa", InputMode.SMART_PHONETIC))
        assertEquals("ඈ", SinhalaEngine.transliterate("AA", InputMode.SMART_PHONETIC))
        assertEquals("ඉ", SinhalaEngine.transliterate("i", InputMode.SMART_PHONETIC))
        assertEquals("ඊ", SinhalaEngine.transliterate("ii", InputMode.SMART_PHONETIC))
        assertEquals("උ", SinhalaEngine.transliterate("u", InputMode.SMART_PHONETIC))
        assertEquals("ඌ", SinhalaEngine.transliterate("uu", InputMode.SMART_PHONETIC))
        assertEquals("ඍ", SinhalaEngine.transliterate("R", InputMode.SMART_PHONETIC))
        assertEquals("ඎ", SinhalaEngine.transliterate("Ru", InputMode.SMART_PHONETIC))
        assertEquals("එ", SinhalaEngine.transliterate("e", InputMode.SMART_PHONETIC))
        assertEquals("ඒ", SinhalaEngine.transliterate("ee", InputMode.SMART_PHONETIC))
        assertEquals("ඓ", SinhalaEngine.transliterate("ai", InputMode.SMART_PHONETIC))
        assertEquals("ඔ", SinhalaEngine.transliterate("o", InputMode.SMART_PHONETIC))
        assertEquals("ඕ", SinhalaEngine.transliterate("oo", InputMode.SMART_PHONETIC))
        assertEquals("ඖ", SinhalaEngine.transliterate("au", InputMode.SMART_PHONETIC))
        assertEquals("ඖ", SinhalaEngine.transliterate("ou", InputMode.SMART_PHONETIC))
    }

    @Test
    fun testConsonants() {
        assertEquals("ක", SinhalaEngine.transliterate("ka", InputMode.SMART_PHONETIC))
        assertEquals("ග", SinhalaEngine.transliterate("ga", InputMode.SMART_PHONETIC))
        assertEquals("ච", SinhalaEngine.transliterate("cha", InputMode.SMART_PHONETIC))
        assertEquals("ජ", SinhalaEngine.transliterate("ja", InputMode.SMART_PHONETIC))
        assertEquals("ට", SinhalaEngine.transliterate("ta", InputMode.SMART_PHONETIC))
        assertEquals("ඩ", SinhalaEngine.transliterate("da", InputMode.SMART_PHONETIC))
        assertEquals("ත", SinhalaEngine.transliterate("tha", InputMode.SMART_PHONETIC))
        assertEquals("ද", SinhalaEngine.transliterate("dha", InputMode.SMART_PHONETIC))
        assertEquals("ද", SinhalaEngine.transliterate("qa", InputMode.SMART_PHONETIC))
        assertEquals("න", SinhalaEngine.transliterate("na", InputMode.SMART_PHONETIC))
        assertEquals("ණ", SinhalaEngine.transliterate("Na", InputMode.SMART_PHONETIC))
        assertEquals("ප", SinhalaEngine.transliterate("pa", InputMode.SMART_PHONETIC))
        assertEquals("බ", SinhalaEngine.transliterate("ba", InputMode.SMART_PHONETIC))
        assertEquals("ම", SinhalaEngine.transliterate("ma", InputMode.SMART_PHONETIC))
        assertEquals("ය", SinhalaEngine.transliterate("ya", InputMode.SMART_PHONETIC))
        assertEquals("ර", SinhalaEngine.transliterate("ra", InputMode.SMART_PHONETIC))
        assertEquals("ල", SinhalaEngine.transliterate("la", InputMode.SMART_PHONETIC))
        assertEquals("ළ", SinhalaEngine.transliterate("La", InputMode.SMART_PHONETIC))
        assertEquals("ව", SinhalaEngine.transliterate("wa", InputMode.SMART_PHONETIC))
        assertEquals("ව", SinhalaEngine.transliterate("va", InputMode.SMART_PHONETIC))
        assertEquals("ස", SinhalaEngine.transliterate("sa", InputMode.SMART_PHONETIC))
        assertEquals("ශ", SinhalaEngine.transliterate("sha", InputMode.SMART_PHONETIC))
        assertEquals("ෂ", SinhalaEngine.transliterate("Sa", InputMode.SMART_PHONETIC))
        assertEquals("ෂ", SinhalaEngine.transliterate("Sha", InputMode.SMART_PHONETIC))
        assertEquals("හ", SinhalaEngine.transliterate("ha", InputMode.SMART_PHONETIC))
        assertEquals("ෆ", SinhalaEngine.transliterate("fa", InputMode.SMART_PHONETIC))
    }

    @Test
    fun testMahapranaAndSanyaka() {
        assertEquals("ඛ", SinhalaEngine.transliterate("kha", InputMode.SMART_PHONETIC))
        assertEquals("ඝ", SinhalaEngine.transliterate("gha", InputMode.SMART_PHONETIC))
        assertEquals("ඡ", SinhalaEngine.transliterate("chha", InputMode.SMART_PHONETIC))
        assertEquals("ඨ", SinhalaEngine.transliterate("Ta", InputMode.SMART_PHONETIC))
        assertEquals("ඪ", SinhalaEngine.transliterate("Da", InputMode.SMART_PHONETIC))
        assertEquals("ථ", SinhalaEngine.transliterate("thha", InputMode.SMART_PHONETIC))
        assertEquals("ධ", SinhalaEngine.transliterate("dhha", InputMode.SMART_PHONETIC))
        assertEquals("ඵ", SinhalaEngine.transliterate("pha", InputMode.SMART_PHONETIC))
        assertEquals("භ", SinhalaEngine.transliterate("bha", InputMode.SMART_PHONETIC))
        assertEquals("ඹ", SinhalaEngine.transliterate("Ba", InputMode.SMART_PHONETIC))
        assertEquals("ඟ", SinhalaEngine.transliterate("zga", InputMode.SMART_PHONETIC))
        assertEquals("ඟ", SinhalaEngine.transliterate("nnga", InputMode.SMART_PHONETIC))
        assertEquals("ඦ", SinhalaEngine.transliterate("zja", InputMode.SMART_PHONETIC))
        assertEquals("ඦ", SinhalaEngine.transliterate("nnja", InputMode.SMART_PHONETIC))
        assertEquals("ඬ", SinhalaEngine.transliterate("zda", InputMode.SMART_PHONETIC))
        assertEquals("ඬ", SinhalaEngine.transliterate("nnda", InputMode.SMART_PHONETIC))
        assertEquals("ඳ", SinhalaEngine.transliterate("zdha", InputMode.SMART_PHONETIC))
        assertEquals("ඳ", SinhalaEngine.transliterate("nndha", InputMode.SMART_PHONETIC))
        assertEquals("ඳ", SinhalaEngine.transliterate("zqa", InputMode.SMART_PHONETIC))
        assertEquals("ඤ", SinhalaEngine.transliterate("zka", InputMode.SMART_PHONETIC))
        assertEquals("ඤ", SinhalaEngine.transliterate("nnya", InputMode.SMART_PHONETIC))
        assertEquals("ඥ", SinhalaEngine.transliterate("zha", InputMode.SMART_PHONETIC))
        assertEquals("ඥ", SinhalaEngine.transliterate("jNa", InputMode.SMART_PHONETIC))
        assertEquals("ග්න", SinhalaEngine.transliterate("gna", InputMode.SMART_PHONETIC))
        assertEquals("ඹ", SinhalaEngine.transliterate("nnba", InputMode.SMART_PHONETIC))
        assertEquals("ඹ", SinhalaEngine.transliterate("zba", InputMode.SMART_PHONETIC))
        assertEquals("ළු", SinhalaEngine.transliterate("Lu", InputMode.SMART_PHONETIC))
    }

    @Test
    fun testPillamAndClusters() {
        assertEquals("ක්", SinhalaEngine.transliterate("k", InputMode.SMART_PHONETIC))
        assertEquals("ක", SinhalaEngine.transliterate("ka", InputMode.SMART_PHONETIC))
        assertEquals("කා", SinhalaEngine.transliterate("kaa", InputMode.SMART_PHONETIC))
        assertEquals("කැ", SinhalaEngine.transliterate("kA", InputMode.SMART_PHONETIC))
        assertEquals("කැ", SinhalaEngine.transliterate("kae", InputMode.SMART_PHONETIC))
        assertEquals("කෑ", SinhalaEngine.transliterate("kAa", InputMode.SMART_PHONETIC))
        assertEquals("කෑ", SinhalaEngine.transliterate("kAA", InputMode.SMART_PHONETIC))
        assertEquals("කි", SinhalaEngine.transliterate("ki", InputMode.SMART_PHONETIC))
        assertEquals("කී", SinhalaEngine.transliterate("kii", InputMode.SMART_PHONETIC))
        assertEquals("කු", SinhalaEngine.transliterate("ku", InputMode.SMART_PHONETIC))
        assertEquals("කූ", SinhalaEngine.transliterate("kuu", InputMode.SMART_PHONETIC))
        assertEquals("කෘ", SinhalaEngine.transliterate("kru", InputMode.SMART_PHONETIC))
        assertEquals("කෲ", SinhalaEngine.transliterate("kruu", InputMode.SMART_PHONETIC))
        assertEquals("කෙ", SinhalaEngine.transliterate("ke", InputMode.SMART_PHONETIC))
        assertEquals("කේ", SinhalaEngine.transliterate("kee", InputMode.SMART_PHONETIC))
        assertEquals("කෛ", SinhalaEngine.transliterate("kai", InputMode.SMART_PHONETIC))
        assertEquals("කො", SinhalaEngine.transliterate("ko", InputMode.SMART_PHONETIC))
        assertEquals("කෝ", SinhalaEngine.transliterate("koo", InputMode.SMART_PHONETIC))
        assertEquals("කෞ", SinhalaEngine.transliterate("kau", InputMode.SMART_PHONETIC))
        assertEquals("කඃ", SinhalaEngine.transliterate("kaH", InputMode.SMART_PHONETIC))
        assertEquals("කං", SinhalaEngine.transliterate("kax", InputMode.SMART_PHONETIC))
        assertEquals("කං", SinhalaEngine.transliterate("kazn", InputMode.SMART_PHONETIC))
        assertEquals("කඞ", SinhalaEngine.transliterate("kaX", InputMode.SMART_PHONETIC))
        assertEquals("ක්‍ය", SinhalaEngine.transliterate("kya", InputMode.SMART_PHONETIC))
        assertEquals("ක්‍ර", SinhalaEngine.transliterate("kra", InputMode.SMART_PHONETIC))
        // Verify ng transliteration produces න්ග් not ඟ්
        assertEquals("න්ග්", SinhalaEngine.transliterate("ng", InputMode.SMART_PHONETIC))
        assertEquals("න්ග", SinhalaEngine.transliterate("nga", InputMode.SMART_PHONETIC))
        assertEquals("ම්බ", SinhalaEngine.transliterate("mba", InputMode.SMART_PHONETIC))
        assertEquals("න්ඩ", SinhalaEngine.transliterate("nda", InputMode.SMART_PHONETIC))
        assertEquals("න්ද", SinhalaEngine.transliterate("ndha", InputMode.SMART_PHONETIC))
    }

    @Test
    fun testSlashboardPhoneticParserObject() {
        assertEquals("අම්මා", slashboardPhoneticParser.parse("ammaa"))
        assertEquals("තාත්තා", slashboardPhoneticParser.parse("thaaththaa"))
        assertEquals("මම ඔයාට ආදරෙයි", slashboardPhoneticParser.parse("mama oyaata aadhareyi"))
    }
}
