import re

with open('app/src/main/java/org/slashboard/ime/ime/SlashboardInputMethodService.kt', 'r') as f:
    text = f.read()

replacement = """            onError = { error ->
                currentInputConnection?.finishComposingText()
                if (error == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    val intent = android.content.Intent(this, org.slashboard.ime.settings.PermissionActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    android.widget.Toast.makeText(this, "Please grant microphone permission", android.widget.Toast.LENGTH_SHORT).show()
                }
            },"""

text = re.sub(r'            onError = \{ error ->\s*currentInputConnection\?\.finishComposingText\(\)\s*if \(error == android\.speech\.SpeechRecognizer\.ERROR_INSUFFICIENT_PERMISSIONS\) \{\s*Toast\.makeText\(this, "Microphone permission required for voice input", Toast\.LENGTH_SHORT\)\.show\(\)\s*\}\s*\},', replacement, text)

with open('app/src/main/java/org/slashboard/ime/ime/SlashboardInputMethodService.kt', 'w') as f:
    f.write(text)
