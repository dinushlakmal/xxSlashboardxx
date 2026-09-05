fun main() {
    val orig = "🤷‍♀"
    val tone = ""
    val toneable = setOf(0x1F44D,0x1F44E,0x1F44F,0x1F64F,0x1F4AA,0x1F44B,0x1F91D,0x1FAF6)
    val cps = orig.codePoints().toArray()
    val out = StringBuilder()
    cps.forEach { cp -> 
        out.appendCodePoint(cp)
        if (cp in toneable) out.append(tone) 
    }
    val res = out.toString()
    println(res.map { String.format("%04X", it.toInt()) }.joinToString(" "))
}
