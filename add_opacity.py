import re

# 1. CustomThemeManager.kt
with open('app/src/main/java/org/slashboard/ime/settings/theme/CustomThemeManager.kt', 'r') as f:
    text = f.read()

text = text.replace('val blurEffect: Boolean = false,', 'val blurEffect: Boolean = false,\n    val keyOpacity: Float = 1.0f,')
text = text.replace('blurEffect = json.optBoolean("blurEffect", false),', 'blurEffect = json.optBoolean("blurEffect", false),\n            keyOpacity = json.optDouble("keyOpacity", 1.0).toFloat(),')

with open('app/src/main/java/org/slashboard/ime/settings/theme/CustomThemeManager.kt', 'w') as f:
    f.write(text)


# 2. KeyboardPalette.kt
with open('app/src/main/java/org/slashboard/ime/ime/KeyboardPalette.kt', 'r') as f:
    text = f.read()

text = text.replace('val blurEffect: Boolean = false,', 'val blurEffect: Boolean = false,\n    val keyOpacity: Float = 1.0f,')
text = text.replace('custom.blurEffect, custom.backgroundImagePath', 'custom.blurEffect, custom.backgroundImagePath, custom.keyOpacity')

# add keyOpacity to custom method
text = text.replace('private fun custom(bg: String, key: String, util: String, ink: String, action: String, actText: String, sel: String, dark: Boolean, hc: Boolean) = KeyboardPalette(', 'private fun custom(bg: String, key: String, util: String, ink: String, action: String, actText: String, sel: String, dark: Boolean, hc: Boolean, keyOpacity: Float = 1.0f) = KeyboardPalette(')
text = text.replace('dynamic = false\n    )', 'dynamic = false,\n        keyOpacity = keyOpacity\n    )')
text = text.replace('dynamic = true\n            )', 'dynamic = true,\n                keyOpacity = 1.0f\n            )')

with open('app/src/main/java/org/slashboard/ime/ime/KeyboardPalette.kt', 'w') as f:
    f.write(text)


# 3. KeyCap.kt
with open('app/src/main/java/org/slashboard/ime/ime/KeyCap.kt', 'r') as f:
    text = f.read()

text = text.replace('val keyRadiusDp: Float? = null', 'val keyRadiusDp: Float? = null,\n    val keyOpacity: Float = 1.0f')
text = text.replace('var colors: KeyboardColors = KeyboardColors(0, 0, 0, 0, 0, false, false, null)', 'var colors: KeyboardColors = KeyboardColors(0, 0, 0, 0, 0, false, false, null, 1.0f)')

# apply opacity in onDraw
opacity_replacement = """        val currentHighlight = if (pressed) 1f else highlightAlpha
        val baseAlpha = (colors.keyOpacity * 255).toInt().coerceIn(0, 255)
        val finalBase = ColorUtils.setAlphaComponent(base, baseAlpha)
        fill.color = if (currentHighlight > 0f) ColorUtils.blendARGB(finalBase, if (colors.dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(), 0.18f * currentHighlight) else finalBase"""
text = text.replace('val currentHighlight = if (pressed) 1f else highlightAlpha\n        fill.color = if (currentHighlight > 0f) ColorUtils.blendARGB(base, if (colors.dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(), 0.18f * currentHighlight) else base', opacity_replacement)

with open('app/src/main/java/org/slashboard/ime/ime/KeyCap.kt', 'w') as f:
    f.write(text)

