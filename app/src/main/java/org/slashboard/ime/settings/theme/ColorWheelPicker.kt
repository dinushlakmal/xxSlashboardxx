package org.slashboard.ime.settings.theme

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.SweepGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.*

@Composable
fun ColorPickerDialog(
    initialHex: String,
    title: String = "Pick Color",
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    // HSV state
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }

    // RGB state
    var redInt by remember { mutableStateOf(0) }
    var greenInt by remember { mutableStateOf(0) }
    var blueInt by remember { mutableStateOf(0) }

    var hexText by remember { mutableStateOf(initialHex.uppercase()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Wheel, 1: RGB, 2: Presets

    // Initialize from initialHex
    LaunchedEffect(initialHex) {
        try {
            val colorInt = AndroidColor.parseColor(initialHex)
            redInt = AndroidColor.red(colorInt)
            greenInt = AndroidColor.green(colorInt)
            blueInt = AndroidColor.blue(colorInt)

            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(colorInt, hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            hexText = String.format("#%02X%02X%02X", redInt, greenInt, blueInt)
        } catch (e: Exception) {
            hue = 200f
            saturation = 0.8f
            value = 0.9f
        }
    }

    fun updateFromHSV(h: Float, s: Float, v: Float) {
        hue = h.coerceIn(0f, 360f)
        saturation = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)

        val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        redInt = AndroidColor.red(colorInt)
        greenInt = AndroidColor.green(colorInt)
        blueInt = AndroidColor.blue(colorInt)
        hexText = String.format("#%02X%02X%02X", redInt, greenInt, blueInt)
    }

    fun updateFromRGB(r: Int, g: Int, b: Int) {
        redInt = r.coerceIn(0, 255)
        greenInt = g.coerceIn(0, 255)
        blueInt = b.coerceIn(0, 255)

        val colorInt = AndroidColor.rgb(redInt, greenInt, blueInt)
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(colorInt, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexText = String.format("#%02X%02X%02X", redInt, greenInt, blueInt)
    }

    fun updateFromHex(hex: String) {
        hexText = hex.uppercase()
        try {
            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
            if (cleanHex.length == 7 || cleanHex.length == 9) {
                val colorInt = AndroidColor.parseColor(cleanHex)
                redInt = AndroidColor.red(colorInt)
                greenInt = AndroidColor.green(colorInt)
                blueInt = AndroidColor.blue(colorInt)

                val hsv = FloatArray(3)
                AndroidColor.colorToHSV(colorInt, hsv)
                hue = hsv[0]
                saturation = hsv[1]
                value = hsv[2]
            }
        } catch (e: Exception) {
            // keep typing
        }
    }

    val currentColor = remember(redInt, greenInt, blueInt) {
        Color(redInt, greenInt, blueInt)
    }

    val initialColor = remember(initialHex) {
        try {
            Color(AndroidColor.parseColor(initialHex))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    // Comparison Preview (Initial vs New)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(initialColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Text("➔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                // Mode Selector Tabs (Color Wheel / RGB / Presets)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Wheel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("RGB Mod", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Circular Color Wheel (HSV)
                        CircularColorWheel(
                            hue = hue,
                            saturation = saturation,
                            onHueSaturationChange = { newHue, newSat ->
                                updateFromHSV(newHue, newSat, value)
                            },
                            modifier = Modifier
                                .size(210.dp)
                                .padding(6.dp)
                        )

                        // Brightness / Value Slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Brightness / Lightness",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${(value * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = value,
                                onValueChange = { newVal ->
                                    updateFromHSV(hue, saturation, newVal)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    1 -> {
                        // RGB Mode Sliders
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RgbSliderRow(
                                label = "Red (R)",
                                value = redInt,
                                activeColor = Color(0xFFEF4444),
                                onValueChange = { updateFromRGB(it, greenInt, blueInt) }
                            )
                            RgbSliderRow(
                                label = "Green (G)",
                                value = greenInt,
                                activeColor = Color(0xFF10B981),
                                onValueChange = { updateFromRGB(redInt, it, blueInt) }
                            )
                            RgbSliderRow(
                                label = "Blue (B)",
                                value = blueInt,
                                activeColor = Color(0xFF3B82F6),
                                onValueChange = { updateFromRGB(redInt, greenInt, it) }
                            )
                        }
                    }
                    2 -> {
                        // Preset Color Swatches
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        ) {
                            Text(
                                "Popular Curated Colors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val presetColors = remember {
                                listOf(
                                    "#0F172A", "#1E293B", "#334155", "#000000", "#121212", "#181825",
                                    "#FFFFFF", "#F8FAFC", "#F1F5F9", "#E2E8F0", "#CBD5E1", "#94A3B8",
                                    "#38BDF8", "#0284C7", "#0072FF", "#2563EB", "#1D4ED8", "#00F0FF",
                                    "#10B981", "#059669", "#064E3B", "#22C55E", "#4ADE80", "#64FFDA",
                                    "#F59E0B", "#D97706", "#F97316", "#EA580C", "#EF4444", "#DC2626",
                                    "#881337", "#E11D48", "#EC4899", "#DB2777", "#F43F5E", "#FF003C",
                                    "#8B5CF6", "#7C3AED", "#6D28D9", "#CBA6F7", "#A855F7", "#9333EA",
                                    "#FBBF24", "#FDE047", "#FEF08A", "#D4AF37", "#FFD700", "#EAB308"
                                )
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 36.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(presetColors) { hex ->
                                    val isSelected = hexText.equals(hex, ignoreCase = true)
                                    val swatchColor = try { Color(AndroidColor.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                updateFromHex(hex)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (value < 0.5f || (redInt + greenInt + blueInt) < 300) Color.White else Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Hex Code Input Row with Copy Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { updateFromHex(it) },
                        label = { Text("HEX Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(hexText))
                        }
                    ) {
                        Text("Copy", fontSize = 12.sp)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onColorSelected(hexText)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Color", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RgbSliderRow(
    label: String,
    value: Int,
    activeColor: Color,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(
                value.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = activeColor
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Interactive HSV Hue & Saturation Canvas Color Disc (Circle).
 * Touch & Drag anywhere inside to select Hue (angle 0-360) and Saturation (radius 0-1).
 */
@Composable
fun CircularColorWheel(
    hue: Float,
    saturation: Float,
    onHueSaturationChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = min(centerX, centerY)

                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)

                        val sat = (dist / radius).coerceIn(0f, 1f)
                        var angle = (atan2(dy, dx) * 180f / PI).toFloat()
                        if (angle < 0) angle += 360f

                        onHueSaturationChange(angle, sat)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = min(centerX, centerY)

                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)

                        val sat = (dist / radius).coerceIn(0f, 1f)
                        var angle = (atan2(dy, dx) * 180f / PI).toFloat()
                        if (angle < 0) angle += 360f

                        onHueSaturationChange(angle, sat)
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = min(centerX, centerY)

            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas

                // 1. Draw SweepGradient for 360-degree Hues
                val colors = intArrayOf(
                    AndroidColor.RED,
                    AndroidColor.YELLOW,
                    AndroidColor.GREEN,
                    AndroidColor.CYAN,
                    AndroidColor.BLUE,
                    AndroidColor.MAGENTA,
                    AndroidColor.RED
                )
                val sweepShader = SweepGradient(centerX, centerY, colors, null)

                // 2. Draw RadialGradient for Saturation (Center = White, Outer = Transparent)
                val radialColors = intArrayOf(AndroidColor.WHITE, AndroidColor.TRANSPARENT)
                val radialShader = RadialGradient(
                    centerX, centerY, radius,
                    radialColors, null, Shader.TileMode.CLAMP
                )

                // Paint with sweep shader
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = sweepShader
                }
                nativeCanvas.drawCircle(centerX, centerY, radius, paint)

                // Paint with radial shader on top
                val satPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = radialShader
                }
                nativeCanvas.drawCircle(centerX, centerY, radius, satPaint)

                // Outer boundary stroke
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3.dp.toPx()
                    color = AndroidColor.argb(70, 255, 255, 255)
                }
                nativeCanvas.drawCircle(centerX, centerY, radius, borderPaint)
            }

            // Draw Selector Thumb Indicator
            val rad = Math.toRadians(hue.toDouble())
            val thumbDistance = saturation * radius
            val thumbX = centerX + (thumbDistance * cos(rad)).toFloat()
            val thumbY = centerY + (thumbDistance * sin(rad)).toFloat()

            val selectedColorInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f))
            val selectedColor = Color(selectedColorInt)

            // Outer drop shadow ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 12.dp.toPx(),
                center = Offset(thumbX, thumbY + 1.dp.toPx())
            )
            // White ring
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
            // Inner chosen color
            drawCircle(
                color = selectedColor,
                radius = 7.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
    }
}
