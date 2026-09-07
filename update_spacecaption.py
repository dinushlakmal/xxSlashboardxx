import re

with open('app/src/main/java/org/slashboard/ime/ime/KeyboardView.kt', 'r') as f:
    text = f.read()

replacement = """    private fun spaceCaption(): String {
        val custom = prefs.customSpacebarText.trim().take(12)
        if (custom.isNotEmpty()) return custom
        return if (editorLayout != EditorLayout.TEXT) "English" else if (prefs.useEnglish) "Slashboard - English" else "Slashboard - ${mode.title}"
    }"""

text = re.sub(r'    private fun spaceCaption\(\) = if \(editorLayout != EditorLayout\.TEXT\) "English" else if \(prefs\.useEnglish\) "Slashboard - English" else "Slashboard - \$\{mode\.title\}"', replacement, text)

with open('app/src/main/java/org/slashboard/ime/ime/KeyboardView.kt', 'w') as f:
    f.write(text)

