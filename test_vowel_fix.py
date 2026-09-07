import re

with open('app/src/main/java/org/slashboard/ime/engine/SinhalaEngine.kt', 'r') as f:
    content = f.read()

# Remove the special 'x'/'M' logic from transliterateWith
replacement = r"""            if (smart && source.startsWith("zn", i)) { out.append("ං"); i += 2; continue }
            if (smart && ch == 'z' && listOf("zg", "zj", "zd", "zdh", "zq", "zk", "zh").none { source.startsWith(it, i) }) { i++; continue }"""
pattern = r"            if \(ch == 'M' \|\| ch == 'x' \|\| \(smart && source\.startsWith\(\"zn\", i\)\)\) \{\n                out\.append\(\"ං\"\)\n                i \+= if \(smart && source\.startsWith\(\"zn\", i\)\) 2 else 1\n                continue\n            \}\n            if \(smart && ch == 'z' && listOf\(\"zg\", \"zj\", \"zd\", \"zdh\", \"zq\", \"zk\", \"zh\"\)\.none \{ source\.startsWith\(it, i\) \}\) \{ i\+\+; continue \}"

content = re.sub(pattern, replacement, content)

# Add M, x, zn to smartVowels
vowels_replacement = """        Vowel("e", "එ", "ෙ"), Vowel("o", "ඔ", "ො"), Vowel("R", "ඍ", null),
        Vowel("x", "ං", "ං"), Vowel("M", "ං", "ං"), Vowel("zn", "ං", "ං")
    )"""
vowels_pattern = r"        Vowel\(\"e\", \"එ\", \"ෙ\"\), Vowel\(\"o\", \"ඔ\", \"ො\"\), Vowel\(\"R\", \"ඍ\", null\)\n    \)"

content = re.sub(vowels_pattern, vowels_replacement, content)

with open('app/src/main/java/org/slashboard/ime/engine/SinhalaEngine.kt', 'w') as f:
    f.write(content)

