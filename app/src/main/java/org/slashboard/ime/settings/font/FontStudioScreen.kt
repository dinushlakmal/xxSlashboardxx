package org.slashboard.ime.settings.font

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.slashboard.ime.settings.KeyboardPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontStudioScreen(
    prefs: KeyboardPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var previewText by remember { mutableStateOf("Slashboard Keyboard 2026") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableFloatStateOf(18f) }
    var activeFontId by remember { mutableStateOf(prefs.keyboardFont) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var isGridMode by remember { mutableStateOf(true) }

    val allFonts = remember(refreshKey) {
        CustomFontManager.getAllFonts(context)
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val imported = CustomFontManager.importFont(context, uri)
            if (imported != null) {
                Toast.makeText(context, "Font imported: ${imported.name}", Toast.LENGTH_SHORT).show()
                refreshKey++
                selectedCategory = "Custom Imported"
            } else {
                Toast.makeText(context, "Failed to import font", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredFonts = remember(allFonts, selectedCategory, searchQuery) {
        allFonts.filter { item ->
            val matchesCategory = (selectedCategory == "All") || (item.category == selectedCategory)
            val matchesSearch = searchQuery.isEmpty() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Font Studio", fontWeight = FontWeight.Bold)
                        Text(
                            "${allFonts.size}+ Custom Fonts & Styles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { fontPickerLauncher.launch("*/*") },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import .TTF", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Interactive Preview Header Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = previewText,
                        onValueChange = { previewText = it },
                        label = { Text("Custom Preview Text") },
                        placeholder = { Text("Type any text to test styles...") },
                        singleLine = true,
                        trailingIcon = {
                            if (previewText.isNotEmpty()) {
                                IconButton(onClick = { previewText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Text Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Slashboard 2026",
                            "ආයුබෝවන් ශ්‍රී ලංකා",
                            "සුබ දවසක් වේවා",
                            "Hello World ✨",
                            "Special Offer 🔥",
                            "1234567890"
                        ).forEach { preset ->
                            SuggestionChip(
                                onClick = { previewText = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Size: ${fontSizeSp.toInt()}sp",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = fontSizeSp,
                            onValueChange = { fontSizeSp = it },
                            valueRange = 12f..36f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Search Bar & View Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search 30 categories, 500+ fonts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    IconButton(
                        onClick = { isGridMode = !isGridMode },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            if (isGridMode) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle Grid/List View",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 30 Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomFontManager.CATEGORIES.forEach { category ->
                    val isSelected = (selectedCategory == category)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Compact Font Grid / List
            LazyVerticalGrid(
                columns = if (isGridMode) GridCells.Adaptive(minSize = 155.dp) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFonts, key = { it.id }) { item ->
                    val isApplied = (activeFontId == item.id)
                    val transformedText = remember(item, previewText) {
                        try {
                            item.transformer(previewText.ifEmpty { "Slashboard" })
                        } catch (e: Exception) {
                            previewText
                        }
                    }
                    val sampleFontFamily = remember(item.id) {
                        try {
                            val tf = CustomFontManager.getTypeface(context, item.id)
                            FontFamily(tf)
                        } catch (e: Exception) {
                            FontFamily.Default
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                prefs.keyboardFont = item.id
                                activeFontId = item.id
                                Toast.makeText(context, "Applied: ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isApplied)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (isApplied)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Header: Name & Active Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isApplied) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            item.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }

                            // Live Stylized Font Sample Box
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = transformedText,
                                            fontSize = (fontSizeSp * 0.85f).coerceIn(12f, 24f).sp,
                                            fontFamily = sampleFontFamily,
                                            fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            // Action Buttons (1-Tap Apply & Copy)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Styled Text", transformedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied: $transformedText", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copy", fontSize = 10.5.sp)
                                }

                                Button(
                                    onClick = {
                                        prefs.keyboardFont = item.id
                                        activeFontId = item.id
                                        Toast.makeText(context, "Applied ${item.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = if (isApplied) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                             else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(
                                        if (isApplied) Icons.Default.Check else Icons.Default.Keyboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (isApplied) "Active" else "Apply", fontSize = 10.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
