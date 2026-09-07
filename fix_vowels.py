import re

with open('app/src/main/java/org/slashboard/ime/engine/SinhalaEngine.kt', 'r') as f:
    content = f.read()

replacement = """        Vowel("u", "උ", "ු"), Vowel("e", "එ", "ෙ"), Vowel("o", "ඔ", "ො"), Vowel("R", "ඍ", null),
        Vowel("x", "ං", "ං"), Vowel("M", "ං", "ං"), Vowel("zn", "ං", "ං")
    )"""
pattern = r"        Vowel\(\"u\", \"උ\", \"ු\"\), Vowel\(\"e\", \"එ\", \"ෙ\"\), Vowel\(\"o\", \"ඔ\", \"ො\"\), Vowel\(\"R\", \"ඍ\", null\)\n    \)"

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/org/slashboard/ime/engine/SinhalaEngine.kt', 'w') as f:
    f.write(content)
