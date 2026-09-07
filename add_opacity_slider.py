import re

with open('app/src/main/java/org/slashboard/ime/settings/theme/ThemeCreatorScreen.kt', 'r') as f:
    text = f.read()

# Add state
text = text.replace('var blurEffect by remember { mutableStateOf(false) }', 'var blurEffect by remember { mutableStateOf(false) }\n    var keyOpacity by remember { mutableStateOf(1.0f) }')

# Add to JSON
text = text.replace('put("blurEffect", blurEffect)', 'put("blurEffect", blurEffect)\n                        put("keyOpacity", keyOpacity.toDouble())')

# Add Slider UI
slider_ui = """
            Text("Key Background Opacity: ${(keyOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = keyOpacity,
                onValueChange = { keyOpacity = it },
                valueRange = 0f..1f
            )
            
            Row(verticalAlignment = Alignment.CenterVertically,"""
text = text.replace('Row(verticalAlignment = Alignment.CenterVertically,', slider_ui, 1)

with open('app/src/main/java/org/slashboard/ime/settings/theme/ThemeCreatorScreen.kt', 'w') as f:
    f.write(text)

