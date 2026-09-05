package org.slashboard.ime.ime

import android.content.Context
import android.util.AttributeSet
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
        // Wrap with your app's theme if available, e.g., SlashboardTheme
        ComposeKeyboard(
            onKeyPressed = { key ->
                // Example integration with existing KeyboardActions
                when (key) {
                    "Space" -> actions?.onSpace()
                    "Enter" -> actions?.onEnter()
                    "Delete" -> actions?.onBackspace(false)
                    "Shift" -> { /* No direct interface method, usually internal state */ }
                    "?123" -> { /* No direct interface method, usually switches layer */ }
                    else -> if (key.length == 1) actions?.onCharacter(key)
                }
            }
        )
    }
}
