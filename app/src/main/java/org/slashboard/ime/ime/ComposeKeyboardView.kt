package org.slashboard.ime.ime

import android.content.Context
import android.util.AttributeSet
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import org.slashboard.ime.settings.KeyboardPreferences

/**
 * A wrapper to expose the ComposeKeyboard to the Android View system (e.g., SlashboardInputMethodService).
 */
class ComposeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val prefs: KeyboardPreferences? = null,
    private val actions: KeyboardActions? = null
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        MaterialTheme {
            ComposeKeyboard(
                onCommitText = { text ->
                    actions?.onCharacter(text)
                },
                onBackspace = { word ->
                    actions?.onBackspace(word)
                },
                onSpace = {
                    actions?.onSpace()
                },
                onEnter = {
                    actions?.onEnter()
                },
                onLanguageToggle = {
                    actions?.onGlobe()
                }
            )
        }
    }
}


