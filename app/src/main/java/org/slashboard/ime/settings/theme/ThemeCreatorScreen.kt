package org.slashboard.ime.settings.theme

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import org.slashboard.ime.settings.KeyboardPreferences
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCreatorScreen(
    prefs: KeyboardPreferences,
    themeIdToEdit: String?,
    onThemeCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("My Theme") }
    var backgroundHex by remember { mutableStateOf("#0F172A") }
    var keyHex by remember { mutableStateOf("#1E293B") }
    var actionHex by remember { mutableStateOf("#0284C7") }
    var utilityHex by remember { mutableStateOf("#1E293B") }
    var textHex by remember { mutableStateOf("#FFFFFF") }
    var inkHex by remember { mutableStateOf("#38BDF8") }
    var spaceKeyHex by remember { mutableStateOf("#1E293B") }
    var spaceBorderHex by remember { mutableStateOf("#38BDF8") }
    
    var dark by remember { mutableStateOf(true) }
    var blurEffect by remember { mutableStateOf(false) }
    var keyOpacity by remember { mutableStateOf(1.0f) }
    var keyRadiusDp by remember { mutableStateOf(12f) }
    var bgImagePath by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(themeIdToEdit) {
        if (themeIdToEdit != null) {
            if (themeIdToEdit.startsWith("custom_")) {
                val data = CustomThemeManager.getTheme(context, themeIdToEdit)
                if (data != null) {
                    name = data.name
                    backgroundHex = data.background
                    keyHex = data.key
                    actionHex = data.action
                    utilityHex = data.utility
                    textHex = data.actionText
                    inkHex = data.ink
                    spaceKeyHex = data.spaceKey ?: data.key
                    spaceBorderHex = data.spaceBorder ?: data.ink
                    dark = data.dark
                    blurEffect = data.blurEffect
                    keyOpacity = data.keyOpacity
                    keyRadiusDp = data.keyRadiusDp ?: 12f
                    bgImagePath = data.backgroundImagePath
                }
            } else {
                val palette = org.slashboard.ime.ime.KeyboardPaletteResolver.resolve(context, themeIdToEdit, prefs.highContrast)
                name = themeIdToEdit.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " (Custom)"
                val hex = { c: Int -> String.format("#%08X", c) }
                backgroundHex = hex(palette.background)
                keyHex = hex(palette.key)
                actionHex = hex(palette.action)
                utilityHex = hex(palette.utility)
                textHex = hex(palette.actionText)
                inkHex = hex(palette.ink)
                spaceKeyHex = palette.spaceKey?.let { hex(it) } ?: hex(palette.key)
                spaceBorderHex = palette.spaceBorder?.let { hex(it) } ?: hex(palette.ink)
                dark = palette.dark
                blurEffect = palette.blurEffect
                keyOpacity = palette.keyOpacity
                keyRadiusDp = palette.keyRadiusDp ?: 12f
                bgImagePath = palette.backgroundImagePath
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(    
        com.canhub.cropper.CropImageContract()    
    ) { result ->    
        if (result.isSuccessful) {    
            val uri = result.uriContent    
            if (uri != null) {    
                try {    
                    val inputStream = context.contentResolver.openInputStream(uri)    
                    val imagesDir = File(context.filesDir, "themes/images").apply { mkdirs() }    
                    val destFile = File(imagesDir, "bg_${System.currentTimeMillis()}.jpg")    
                    val outputStream = FileOutputStream(destFile)    
                    inputStream?.copyTo(outputStream)    
                    inputStream?.close()    
                    outputStream.close()    
                    bgImagePath = destFile.absolutePath    
                } catch (e: Exception) {    
                    e.printStackTrace()    
                }    
            }    
        }    
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Theme Creator", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Interactive Keyboard Preview
            Text(
                "Live Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            ThemeLiveKeyboardPreview(
                backgroundHex = backgroundHex,
                keyHex = keyHex,
                actionHex = actionHex,
                utilityHex = utilityHex,
                textHex = textHex,
                inkHex = inkHex,
                spaceKeyHex = spaceKeyHex,
                spaceBorderHex = spaceBorderHex,
                dark = dark,
                blurEffect = blurEffect,
                keyOpacity = keyOpacity,
                keyRadiusDp = keyRadiusDp,
                bgImagePath = bgImagePath
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Theme Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            ColorInputRow("Background Color", backgroundHex) { backgroundHex = it }
            ColorInputRow("Key Color", keyHex) { keyHex = it }
            ColorInputRow("Action Key Color", actionHex) { actionHex = it }
            ColorInputRow("Utility Key Color", utilityHex) { utilityHex = it }
            ColorInputRow("Key Text Color", textHex) { textHex = it }
            ColorInputRow("Accent/Ink Color", inkHex) { inkHex = it }
            ColorInputRow("Space Bar Fill Color", spaceKeyHex) { spaceKeyHex = it }
            ColorInputRow("Space Bar Border Color", spaceBorderHex) { spaceBorderHex = it }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Key Corner Radius: ${keyRadiusDp.toInt()}dp", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = keyRadiusDp,
                onValueChange = { keyRadiusDp = it },
                valueRange = 0f..24f
            )
            
            
            Text("Key Background Opacity: ${(keyOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = keyOpacity,
                onValueChange = { keyOpacity = it },
                valueRange = 0f..1f
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Dark Theme Mode (White Base Text)", modifier = Modifier.weight(1f))
                Switch(checked = dark, onCheckedChange = { dark = it })
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Glass Blur Effect", modifier = Modifier.weight(1f))
                Switch(checked = blurEffect, onCheckedChange = { blurEffect = it })
            }
            
            OutlinedButton(
                onClick = { 
                    imageLauncher.launch(
                        com.canhub.cropper.CropImageContractOptions(
                            uri = null,
                            cropImageOptions = com.canhub.cropper.CropImageOptions().apply {
                                imageSourceIncludeGallery = true
                                imageSourceIncludeCamera = false
                            }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (bgImagePath == null) "Pick Background Image" else "Image Selected (Tap to Change)")
            }
            
            if (bgImagePath != null) {
                TextButton(onClick = { bgImagePath = null }) {
                    Text("Remove Background Image", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val id = if (themeIdToEdit != null && themeIdToEdit.startsWith("custom_")) themeIdToEdit else "custom_" + System.currentTimeMillis()
                    val json = JSONObject().apply {
                        put("name", name)
                        put("background", backgroundHex)
                        put("key", keyHex)
                        put("utility", utilityHex)
                        put("action", actionHex)
                        put("actionText", textHex)
                        put("ink", inkHex)
                        put("spaceKey", spaceKeyHex)
                        put("spaceBorder", spaceBorderHex)
                        put("selected", utilityHex)
                        put("dark", dark)
                        put("keyRadiusDp", keyRadiusDp.toDouble())
                        put("blurEffect", blurEffect)
                        put("keyOpacity", keyOpacity.toDouble())
                        if (bgImagePath != null) put("backgroundImagePath", bgImagePath)
                    }
                    val file = File(CustomThemeManager.getThemesDir(context), "$id.slashtheme")
                    file.writeText(json.toString(2))
                    
                    prefs.theme = id
                    onThemeCreated(id)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save & Apply Theme")
            }
        }
    }
}

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun ThemeLiveKeyboardPreview(
    backgroundHex: String,
    keyHex: String,
    actionHex: String,
    utilityHex: String,
    textHex: String,
    inkHex: String,
    spaceKeyHex: String,
    spaceBorderHex: String,
    dark: Boolean,
    blurEffect: Boolean,
    keyOpacity: Float,
    keyRadiusDp: Float,
    bgImagePath: String?
) {
    val bgColor = remember(backgroundHex) { parseHexColor(backgroundHex, Color(0xFF0F172A)) }
    val keyColor = remember(keyHex, keyOpacity) { parseHexColor(keyHex, Color(0xFF1E293B)).copy(alpha = keyOpacity) }
    val actionColor = remember(actionHex) { parseHexColor(actionHex, Color(0xFF0284C7)) }
    val utilColor = remember(utilityHex, keyOpacity) { parseHexColor(utilityHex, Color(0xFF1E293B)).copy(alpha = keyOpacity) }
    val textColor = remember(textHex) { parseHexColor(textHex, Color.White) }
    val inkColor = remember(inkHex) { parseHexColor(inkHex, Color(0xFF38BDF8)) }
    val context = LocalContext.current
    val spaceLabel = remember {
        val prefText = org.slashboard.ime.settings.KeyboardPreferences(context).customSpacebarText.trim().take(14)
        if (prefText.isNotEmpty()) prefText else "Slashboard"
    }
    val spaceBarColor = remember(spaceKeyHex, keyOpacity) { parseHexColor(spaceKeyHex, Color(0xFF1E293B)).copy(alpha = keyOpacity) }
    val spaceBorderColor = remember(spaceBorderHex) { parseHexColor(spaceBorderHex, Color(0xFF38BDF8)) }

    val bgBitmap = remember(bgImagePath) {
        if (!bgImagePath.isNullOrEmpty()) {
            runCatching {
                android.graphics.BitmapFactory.decodeFile(bgImagePath)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bgBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bgBitmap,
                    contentDescription = "Background",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                if (blurEffect) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Toolbar / Suggestion Rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(utilColor.copy(alpha = (keyOpacity * 0.7f).coerceIn(0.15f, 0.9f)))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("EN", color = inkColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("😀", fontSize = 11.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("මම", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("ගෙදර", color = inkColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("යනවා", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Text("⚙", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }

                // Row 1: Q W E R T Y U I O P
                val r1Keys = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (k in r1Keys) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(keyRadiusDp.dp))
                                .background(keyColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Row 2: A S D F G H J K L
                val r2Keys = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (k in r2Keys) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(keyRadiusDp.dp))
                                .background(keyColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Row 3: Shift, Z X C V B N M, Delete
                val r3Keys = listOf("Z", "X", "C", "V", "B", "N", "M")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(utilColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇧", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    for (k in r3Keys) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(keyRadiusDp.dp))
                                .background(keyColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(utilColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌫", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Row 4: ?123, Globe, Spacebar, ., Enter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(utilColor)
                            .border(1.2.dp, spaceBorderColor, RoundedCornerShape(keyRadiusDp.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?123", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(29.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(utilColor)
                            .border(1.dp, spaceBorderColor.copy(alpha = 0.6f), RoundedCornerShape(keyRadiusDp.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌐", fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(4f)
                            .height(27.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(spaceBarColor)
                            .border(1.2.dp, spaceBorderColor, RoundedCornerShape(keyRadiusDp.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(spaceLabel, color = textColor.copy(alpha = 0.65f), fontSize = 10.sp, maxLines = 1)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(29.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(keyColor)
                            .border(1.dp, spaceBorderColor.copy(alpha = 0.6f), RoundedCornerShape(keyRadiusDp.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(".", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(keyRadiusDp.dp))
                            .background(actionColor)
                            .border(1.2.dp, spaceBorderColor, RoundedCornerShape(keyRadiusDp.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏎", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorInputRow(label: String, hex: String, onHexChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Transparent }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                .clickable { showDialog = true }
        )
        OutlinedTextField(
            value = hex,
            onValueChange = onHexChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    if (showDialog) {
        ColorPickerDialog(
            initialHex = hex,
            onDismiss = { showDialog = false },
            onColorSelected = { 
                onHexChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(initialHex: String, onDismiss: () -> Unit, onColorSelected: (String) -> Unit) {
    var r by remember { mutableStateOf(0f) }
    var g by remember { mutableStateOf(0f) }
    var b by remember { mutableStateOf(0f) }

    LaunchedEffect(initialHex) {
        try {
            val c = android.graphics.Color.parseColor(initialHex)
            r = android.graphics.Color.red(c) / 255f
            g = android.graphics.Color.green(c) / 255f
            b = android.graphics.Color.blue(c) / 255f
        } catch (e: Exception) {}
    }

    val currentColor = Color(r, g, b)
    val hexString = String.format("#%02X%02X%02X", (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Color") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Red", style = MaterialTheme.typography.bodySmall)
                Slider(value = r, onValueChange = { r = it }, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red))
                Text("Green", style = MaterialTheme.typography.bodySmall)
                Slider(value = g, onValueChange = { g = it }, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green))
                Text("Blue", style = MaterialTheme.typography.bodySmall)
                Slider(value = b, onValueChange = { b = it }, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue))
                Text("Hex: $hexString", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onColorSelected(hexString) }) { Text("Select") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
