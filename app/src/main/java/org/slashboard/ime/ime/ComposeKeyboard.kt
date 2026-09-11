package org.slashboard.ime.ime

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Symbol visual hints mapping for English QWERTY layout:
 * - Row 1 (Numbers): q:1, w:2, e:3, r:4, t:5, y:6, u:7, i:8, o:9, p:0
 * - Row 2 (Symbols): a:@, s:#, d:$, f:_, g:&, h:-, j:+, k:(, l:)
 * - Row 3 (Punctuation): z:*, x:", c:', v::, b:;, n:!, m:?
 */
object EnglishKeyboardSymbols {
    val SYMBOL_HINTS: Map<String, String> = mapOf(
        // Row 1 (Numbers)
        "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
        // Row 2 (Symbols)
        "a" to "@", "s" to "#", "d" to "$", "f" to "_", "g" to "&",
        "h" to "-", "j" to "+", "k" to "(", "l" to ")",
        // Row 3 (Punctuation)
        "z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";",
        "n" to "!", "m" to "?"
    )

    val ROW_1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val ROW_2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val ROW_3 = listOf("z", "x", "c", "v", "b", "n", "m")
}

/**
 * Production-ready Android Jetpack Compose Keyboard supporting:
 * - Subtle top-aligned dimmed symbol hints positioned above the main letter (English layout).
 * - Quick tap to commit lowercase/uppercase letters with KEYBOARD_TAP haptic.
 * - Long press to commit assigned top symbol with LONG_PRESS haptic feedback.
 * - Modular architecture easily plugged into any InputMethodService.
 */
@Composable
fun ComposeKeyboard(
    modifier: Modifier = Modifier,
    isShifted: Boolean = false,
    isCapsLock: Boolean = false,
    isSymbolsLayer: Boolean = false,
    onCommitText: (String) -> Unit = {},
    onBackspace: (word: Boolean) -> Unit = {},
    onSpace: () -> Unit = {},
    onEnter: () -> Unit = {},
    onShiftToggle: () -> Unit = {},
    onSymbolsToggle: () -> Unit = {},
    onLanguageToggle: () -> Unit = {}
) {
    // Local state fallbacks for standalone usage
    var localShift by remember { mutableStateOf(isShifted) }
    var localCaps by remember { mutableStateOf(isCapsLock) }
    var localSymbols by remember { mutableStateOf(isSymbolsLayer) }

    val activeShift = isShifted || localShift
    val activeCaps = isCapsLock || localCaps
    val activeSymbols = isSymbolsLayer || localSymbols

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer ?: MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!activeSymbols) {
            // === ENGLISH QWERTY LETTERS WITH TOP SYMBOL HINTS ===

            // Row 1 (q - p with 1 - 0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EnglishKeyboardSymbols.ROW_1.forEach { letter ->
                    val hint = EnglishKeyboardSymbols.SYMBOL_HINTS[letter]
                    val displayLetter = if (activeShift || activeCaps) letter.uppercase() else letter
                    EnglishLetterKey(
                        letter = displayLetter,
                        symbolHint = hint,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onCommitText(displayLetter)
                            if (localShift && !localCaps) localShift = false
                        },
                        onLongPressSymbol = { symbol ->
                            onCommitText(symbol)
                        }
                    )
                }
            }

            // Row 2 (a - l with @, #, $, _, &, -, +, (, ))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EnglishKeyboardSymbols.ROW_2.forEach { letter ->
                    val hint = EnglishKeyboardSymbols.SYMBOL_HINTS[letter]
                    val displayLetter = if (activeShift || activeCaps) letter.uppercase() else letter
                    EnglishLetterKey(
                        letter = displayLetter,
                        symbolHint = hint,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onCommitText(displayLetter)
                            if (localShift && !localCaps) localShift = false
                        },
                        onLongPressSymbol = { symbol ->
                            onCommitText(symbol)
                        }
                    )
                }
            }

            // Row 3 (Shift, z - m with *, ", ', :, ;, !, ?, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shift key
                UtilityKey(
                    modifier = Modifier.weight(1.4f),
                    active = activeShift || activeCaps,
                    onClick = {
                        if (localShift) {
                            localCaps = !localCaps
                            if (!localCaps) localShift = false
                        } else {
                            localShift = true
                        }
                        onShiftToggle()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = if (activeCaps) "Caps Lock Active" else "Shift",
                        tint = if (activeShift || activeCaps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                EnglishKeyboardSymbols.ROW_3.forEach { letter ->
                    val hint = EnglishKeyboardSymbols.SYMBOL_HINTS[letter]
                    val displayLetter = if (activeShift || activeCaps) letter.uppercase() else letter
                    EnglishLetterKey(
                        letter = displayLetter,
                        symbolHint = hint,
                        modifier = Modifier.weight(1f),
                        onTap = {
                            onCommitText(displayLetter)
                            if (localShift && !localCaps) localShift = false
                        },
                        onLongPressSymbol = { symbol ->
                            onCommitText(symbol)
                        }
                    )
                }

                // Delete / Backspace key
                UtilityKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = { onBackspace(false) },
                    onLongPress = { onBackspace(true) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

        } else {
            // === NUMBERS & SYMBOLS LAYER ===
            val symRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            val symRow2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
            val symRow3 = listOf("*", "\"", "'", ":", ";", "!", "?", "%", "=")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                symRow1.forEach { sym ->
                    SimpleSymbolKey(symbol = sym, modifier = Modifier.weight(1f), onClick = { onCommitText(sym) })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                symRow2.forEach { sym ->
                    SimpleSymbolKey(symbol = sym, modifier = Modifier.weight(1f), onClick = { onCommitText(sym) })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                symRow3.forEach { sym ->
                    SimpleSymbolKey(symbol = sym, modifier = Modifier.weight(1f), onClick = { onCommitText(sym) })
                }
                UtilityKey(modifier = Modifier.weight(1.2f), onClick = { onBackspace(false) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Delete")
                }
            }
        }

        // === BOTTOM ROW (LAYER, LANGUAGE, SPACE, ENTER) ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ?123 / ABC Toggle
            UtilityKey(
                modifier = Modifier.weight(1.3f),
                onClick = {
                    localSymbols = !localSymbols
                    onSymbolsToggle()
                }
            ) {
                Text(
                    text = if (activeSymbols) "ABC" else "?123",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Comma / Language toggle
            UtilityKey(
                modifier = Modifier.weight(1.0f),
                onClick = { onCommitText(",") },
                onLongPress = { onLanguageToggle() }
            ) {
                Text(text = ",", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Spacebar
            UtilityKey(
                modifier = Modifier.weight(4.0f),
                onClick = { onSpace() }
            ) {
                Text(
                    text = "English",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }

            // Period (.)
            UtilityKey(
                modifier = Modifier.weight(1.0f),
                onClick = { onCommitText(".") }
            ) {
                Text(text = ".", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Enter Key
            UtilityKey(
                modifier = Modifier.weight(1.4f),
                isAccent = true,
                onClick = { onEnter() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * An individual English key cap that renders:
 * 1. A subtle, dimmed symbol hint at the top-center above the main character.
 * 2. The primary lowercase/uppercase character.
 * 3. Quick tap commits letter with KEYBOARD_TAP haptic.
 * 4. Long press commits symbol with LONG_PRESS haptic feedback.
 */
@Composable
fun EnglishLetterKey(
    letter: String,
    symbolHint: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPressSymbol: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPressSymbol)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val pressedColor = MaterialTheme.colorScheme.surfaceVariant
    val keyBg = if (isPressed) pressedColor else surfaceColor

    val keyShape = RoundedCornerShape(7.dp)

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(50.dp)
            .shadow(if (isPressed) 0.5.dp else 1.5.dp, keyShape)
            .clip(keyShape)
            .background(keyBg)
            .pointerInput(letter, symbolHint) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var longPressFired = false
                    val holdJob = coroutineScope.launch {
                        delay(200L) // Snappy 200ms trigger while holding
                        if (!symbolHint.isNullOrEmpty()) {
                            longPressFired = true
                            runCatching {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }.getOrElse {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            currentOnLongPress(symbolHint)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    holdJob.cancel()
                    isPressed = false
                    if (up != null && !longPressFired) {
                        // Quick Tap
                        runCatching {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }.getOrElse {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        currentOnTap()
                    }
                }
            }
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top-Aligned Subtle Dimmed Symbol Hint
            if (!symbolHint.isNullOrEmpty()) {
                Text(
                    text = symbolHint,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            } else {
                Box(modifier = Modifier.height(10.dp))
            }

            // Primary Main Letter
            Text(
                text = letter,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

/**
 * Simple key for numeric/symbol layers.
 */
@Composable
fun SimpleSymbolKey(
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val keyShape = RoundedCornerShape(7.dp)

    Box(
        modifier = modifier
            .height(50.dp)
            .shadow(if (isPressed) 0.5.dp else 1.5.dp, keyShape)
            .clip(keyShape)
            .background(if (isPressed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .pointerInput(symbol) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        runCatching {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }.getOrElse {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Functional / Utility key (Shift, Backspace, Space, Enter, ?123, etc.).
 */
@Composable
fun UtilityKey(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    isAccent: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val keyShape = RoundedCornerShape(7.dp)

    val normalBg = if (isAccent) {
        MaterialTheme.colorScheme.primary
    } else if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    }

    val pressedBg = if (isAccent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .shadow(if (isPressed) 0.5.dp else 1.5.dp, keyShape)
            .clip(keyShape)
            .background(if (isPressed) pressedBg else normalBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        runCatching {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }.getOrElse {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onClick()
                    },
                    onLongPress = {
                        if (onLongPress != null) {
                            runCatching {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }.getOrElse {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onLongPress()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
