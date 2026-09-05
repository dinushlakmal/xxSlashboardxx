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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
