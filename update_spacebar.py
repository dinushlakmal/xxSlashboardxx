import re

with open('app/src/main/java/org/slashboard/ime/settings/KeyboardPreferences.kt', 'r') as f:
    text = f.read()

replacement = """    var thumbReachMode: String
        get() = store.getString(THUMB_REACH_MODE, "off") ?: "off"
        set(value) = store.edit().putString(THUMB_REACH_MODE, value).apply()

    var customSpacebarText: String
        get() = store.getString(CUSTOM_SPACEBAR_TEXT, "") ?: ""
        set(value) = store.edit().putString(CUSTOM_SPACEBAR_TEXT, value).apply()"""

text = re.sub(r'    var thumbReachMode: String\s+get\(\) = store\.getString\(THUMB_REACH_MODE, "off"\) \?: "off"\s+set\(value\) = store\.edit\(\)\.putString\(THUMB_REACH_MODE, value\)\.apply\(\)', replacement, text)

# add constant
text = re.sub(r'        private const val THUMB_REACH_MODE = "thumb_reach_mode"', '        private const val THUMB_REACH_MODE = "thumb_reach_mode"\n        private const val CUSTOM_SPACEBAR_TEXT = "custom_spacebar_text"', text)

with open('app/src/main/java/org/slashboard/ime/settings/KeyboardPreferences.kt', 'w') as f:
    f.write(text)

