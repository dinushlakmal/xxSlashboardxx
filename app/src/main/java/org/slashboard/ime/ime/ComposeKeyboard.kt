package org.slashboard.ime.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * A basic Compose-based keyboard layout to serve as a foundational UI component.
 */
@Composable
fun ComposeKeyboard(
    keys: List<List<String>> = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Shift", "Z", "X", "C", "V", "B", "N", "M", "Delete"),
        listOf("?123", ",", "Space", ".", "Enter")
    ),
    onKeyPressed: (String) -> Unit
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val keyboardBackground = remember(surfaceVariant) {
        derivedStateOf { surfaceVariant }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithCache {
                onDrawBehind {
                    drawRect(color = keyboardBackground.value)
                }
            }
            .padding(vertical = 8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        label = key,
                        modifier = Modifier
                            .weight(if (key == "Space") 4f else if (key == "Shift" || key == "Delete" || key == "Enter" || key == "?123") 1.5f else 1f)
                            .padding(horizontal = 2.dp),
                        onClick = { onKeyPressed(key) }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()

    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Correctly using derivedStateOf by returning a State object and NOT re-remembering on isPressed
    val backgroundColor = remember(surface, surfaceVariant) {
        derivedStateOf { if (isPressedState.value) surfaceVariant else surface }
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Medium,
        color = onSurface
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple to fully control drawing and prevent UI blocking
                onClick = onClick
            )
            // Implement Modifier.drawBehind and drawWithCache
            .drawBehind {
                drawRoundRect(
                    color = backgroundColor.value,
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
            .drawWithCache {
                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = textStyle
                )
                onDrawWithContent {
                    drawContent()
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = (size.width - textLayoutResult.size.width) / 2f,
                            y = (size.height - textLayoutResult.size.height) / 2f
                        )
                    )
                }
            }
    )
}
