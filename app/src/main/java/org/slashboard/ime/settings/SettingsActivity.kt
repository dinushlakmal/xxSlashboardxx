package org.slashboard.ime.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import org.slashboard.ime.ime.KeyboardPalette
import org.slashboard.ime.ime.KeyboardPaletteResolver
import org.slashboard.ime.engine.InputMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import org.slashboard.ime.BuildConfig
import org.slashboard.ime.R
import org.slashboard.ime.data.LocalLearningStore
import org.slashboard.ime.data.ClipboardHistoryStore
import org.slashboard.ime.ime.TouchPersonalizationStore
import org.slashboard.ime.ime.SlashboardInputMethodService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            com.vanniktech.emoji.EmojiManager.install(com.vanniktech.emoji.ios.IosEmojiProvider())
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = KeyboardPreferences(this)
        
        setContent {
            var themeState by remember { mutableStateOf(prefs.theme) }
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFFFFFF),
                    onPrimary = Color(0xFF0044B3),
                    primaryContainer = Color(0xFF00D2FF).copy(alpha = 0.3f),
                    onPrimaryContainer = Color.White,
                    surface = Color.Black.copy(alpha = 0.35f),
                    onSurface = Color.White,
                    surfaceVariant = Color.Black.copy(alpha = 0.45f),
                    onSurfaceVariant = Color.LightGray,
                    background = Color.Transparent,
                    onBackground = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0052D4),
                                    Color(0xFF0038A8),
                                    Color(0xFF001F6B),
                                    Color(0xFF000E33)
                                )
                            )
                        )
                ) {
                    SettingsScreen(prefs, onThemeChanged = { themeState = it })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: KeyboardPreferences, onThemeChanged: (String) -> Unit = {}) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var showThemesPage by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val imm = remember { context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager }
    val keyboardEnabled = remember(refresh) {
        imm.enabledInputMethodList.any { it.packageName == context.packageName }
    }
    val keyboardSelected = remember(refresh) {
        val selectedStr = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        selectedStr != null && selectedStr.startsWith(context.packageName)
    }

    if (showThemesPage) {
        ThemeLayoutsScreen(
            prefs = prefs,
            onThemeChanged = {
                onThemeChanged(it)
                refresh++
            },
            onBack = {
                showThemesPage = false
                refresh++
            }
        )
        return
    }

    var expandedSection by remember { mutableStateOf<String?>("Setup") }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HeaderAnimation()
            }

            item {
                var testText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    label = { Text("Test Keyboard Here") },
                    placeholder = { Text("Tap here to type and test Slashboard...") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    trailingIcon = {
                        if (testText.isNotEmpty()) {
                            IconButton(onClick = { testText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text")
                            }
                        }
                    }
                )
            }

            if (!keyboardEnabled || !keyboardSelected) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (!keyboardEnabled) "Keyboard Not Enabled" else "Keyboard Not Selected",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = if (!keyboardEnabled) 
                                            "Tap below to enable Slashboard in Android Settings."
                                        else 
                                            "Slashboard is enabled! Tap below to make it your current active keyboard.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (!keyboardEnabled) {
                                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                    } else {
                                        imm.showInputMethodPicker()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (!keyboardEnabled) Icons.Default.Settings else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!keyboardEnabled) "Enable in Settings" else "Select as Active Keyboard",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            item {
                AccordionSection(
                    title = "Setup", 
                    icon = Icons.Default.Settings, 
                    expanded = expandedSection == "Setup" || (!keyboardEnabled || !keyboardSelected), 
                    onExpandedChange = { expandedSection = if (it) "Setup" else null }
                ) {
                    SettingsActionRow(
                        title = "Step 1: " + stringResource(R.string.enable_keyboard),
                        summary = if (keyboardEnabled) stringResource(R.string.status_enabled) else stringResource(R.string.status_enable_needed),
                        icon = Icons.Default.Keyboard,
                        statusBadge = if (keyboardEnabled) "Enabled" else "Required",
                        isCompleted = keyboardEnabled,
                        onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    SettingsActionRow(
                        title = "Step 2: " + stringResource(R.string.select_keyboard),
                        summary = if (keyboardSelected) stringResource(R.string.status_selected) else stringResource(R.string.status_select_needed),
                        icon = Icons.Default.CheckCircle,
                        statusBadge = if (keyboardSelected) "Active" else if (keyboardEnabled) "Tap to Select" else "Pending Step 1",
                        isCompleted = keyboardSelected,
                        onClick = { imm.showInputMethodPicker() }
                    )
                }
            }

            item {
                AccordionSection("Typing", Icons.Default.Keyboard, expanded = expandedSection == "Typing", onExpandedChange = { expandedSection = if (it) "Typing" else null }) {
                    SettingsChoiceRow(
                        title = stringResource(R.string.input_mode),
                        icon = Icons.Default.Language,
                        entries = stringArrayResource(R.array.input_mode_entries).toList(),
                        values = stringArrayResource(R.array.input_mode_values).toList(),
                        currentValue = prefs.mode.name,
                        onValueChange = { 
                            prefs.mode = runCatching { InputMode.valueOf(it) }.getOrDefault(InputMode.SMART_PHONETIC)
                            refresh++ 
                        }
                    )
                    SettingsToggleRow(
                        title = stringResource(R.string.suggestions),
                        summary = null,
                        icon = Icons.Default.AutoFixHigh,
                        checked = prefs.suggestions,
                        onCheckedChange = { prefs.suggestions = it; refresh++ }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.top_row),
                        icon = Icons.Default.Numbers,
                        entries = stringArrayResource(R.array.top_row_entries).toList(),
                        values = stringArrayResource(R.array.top_row_values).toList(),
                        currentValue = prefs.topRow,
                        onValueChange = { prefs.topRow = it; refresh++ }
                    )
                }
            }

            item {
                AccordionSection("Emoji & Clipboard", Icons.Default.EmojiEmotions, expanded = expandedSection == "Emoji & Clipboard", onExpandedChange = { expandedSection = if (it) "Emoji & Clipboard" else null }) {
                    SettingsToggleRow(
                        title = stringResource(R.string.emoji_picker),
                        summary = null,
                        icon = Icons.Default.SentimentSatisfied,
                        checked = prefs.emojiPicker,
                        onCheckedChange = { prefs.emojiPicker = it; refresh++ }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.skin_tone),
                        icon = Icons.Default.Face,
                        entries = stringArrayResource(R.array.skin_tone_entries).toList(),
                        values = stringArrayResource(R.array.skin_tone_values).toList(),
                        currentValue = prefs.skinTone,
                        onValueChange = { prefs.skinTone = it; refresh++ }
                    )
                    SettingsToggleRow(
                        title = stringResource(R.string.clipboard_history),
                        summary = stringResource(R.string.clipboard_summary),
                        icon = Icons.Default.ContentPaste,
                        checked = prefs.clipboardHistory,
                        onCheckedChange = { prefs.clipboardHistory = it; refresh++ }
                    )
                }
            }

            item {
                AccordionSection("Themes & Appearance", Icons.Default.Palette, expanded = expandedSection == "Appearance" || expandedSection == "Themes & Appearance", onExpandedChange = { expandedSection = if (it) "Themes & Appearance" else null }) {
                    SettingsActionRow(
                        title = "Layouts",
                        summary = "Browse and preview theme designs",
                        icon = Icons.Default.Palette,
                        onClick = { showThemesPage = true }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.key_spacing),
                        icon = Icons.Default.SpaceBar,
                        entries = stringArrayResource(R.array.spacing_entries).toList(),
                        values = stringArrayResource(R.array.spacing_values).toList(),
                        currentValue = prefs.keySpacing,
                        onValueChange = { prefs.keySpacing = it; refresh++ }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.keyboard_size),
                        icon = Icons.Default.Height,
                        entries = stringArrayResource(R.array.keyboard_size_entries).toList(),
                        values = stringArrayResource(R.array.keyboard_size_values).toList(),
                        currentValue = prefs.keyboardSize,
                        onValueChange = { prefs.keyboardSize = it; refresh++ }
                    )
                    SettingsToggleRow(
                        title = stringResource(R.string.spatial_decoder),
                        summary = stringResource(R.string.spatial_decoder_summary),
                        icon = Icons.Default.TouchApp,
                        checked = prefs.spatialDecoder,
                        onCheckedChange = { prefs.spatialDecoder = it; refresh++ }
                    )
                    SettingsToggleRow(
                        title = "High Contrast Mode",
                        summary = "Increase contrast on key labels and borders",
                        icon = Icons.Default.Contrast,
                        checked = prefs.highContrast,
                        onCheckedChange = { prefs.highContrast = it; refresh++ }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.one_handed),
                        icon = Icons.Default.PanTool,
                        entries = stringArrayResource(R.array.one_handed_entries).toList(),
                        values = stringArrayResource(R.array.one_handed_values).toList(),
                        currentValue = prefs.oneHanded,
                        onValueChange = { prefs.oneHanded = it; refresh++ }
                    )
                    SettingsToggleRow(
                        title = stringResource(R.string.haptics),
                        summary = null,
                        icon = Icons.Default.Vibration,
                        checked = prefs.haptics,
                        onCheckedChange = { prefs.haptics = it; refresh++ }
                    )
                    SettingsToggleRow(
                        title = "Key Sounds",
                        summary = null,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        checked = prefs.keySounds,
                        onCheckedChange = { prefs.keySounds = it; refresh++ }
                    )
                    SettingsChoiceRow(
                        title = "Sound Pack",
                        icon = Icons.Default.Audiotrack,
                        entries = listOf("System", "iOS", "Mechanical", "Typewriter"),
                        values = listOf("system", "ios", "mechanical", "typewriter"),
                        currentValue = prefs.soundPack,
                        onValueChange = { prefs.soundPack = it; refresh++ }
                    )
                }
            }

            item {
                AccordionSection("Privacy & Reset", Icons.Default.Security, expanded = expandedSection == "Privacy & Reset", onExpandedChange = { expandedSection = if (it) "Privacy & Reset" else null }) {
                    var showDialog by remember { mutableStateOf<String?>(null) }
                    
                    if (showDialog == "clear_learning") {
                        AlertDialog(
                            onDismissRequest = { showDialog = null },
                            title = { Text(stringResource(R.string.clear_learning_title)) },
                            text = { Text(stringResource(R.string.clear_learning_message)) },
                            confirmButton = {
                                TextButton(onClick = { LocalLearningStore(context).clear(); showDialog = null }) { Text(stringResource(R.string.clear)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = null }) { Text(stringResource(android.R.string.cancel)) }
                            }
                        )
                    }
                    if (showDialog == "reset_touch") {
                        AlertDialog(
                            onDismissRequest = { showDialog = null },
                            title = { Text(stringResource(R.string.reset_touch_title)) },
                            text = { Text(stringResource(R.string.reset_touch_message)) },
                            confirmButton = {
                                TextButton(onClick = { 
                                    TouchPersonalizationStore(context).reset()
                                    showDialog = null 
                                }) { Text(stringResource(R.string.clear)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = null }) { Text(stringResource(android.R.string.cancel)) }
                            }
                        )
                    }
                    if (showDialog == "clear_clipboard") {
                        AlertDialog(
                            onDismissRequest = { showDialog = null },
                            title = { Text(stringResource(R.string.clear_clipboard_title)) },
                            text = { Text(stringResource(R.string.clear_clipboard_message)) },
                            confirmButton = {
                                TextButton(onClick = { 
                                    ClipboardHistoryStore(context).clear()
                                    showDialog = null 
                                }) { Text(stringResource(R.string.clear)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = null }) { Text(stringResource(android.R.string.cancel)) }
                            }
                        )
                    }
                    if (showDialog == "reset_all") {
                        AlertDialog(
                            onDismissRequest = { showDialog = null },
                            title = { Text(stringResource(R.string.reset_title)) },
                            text = { Text(stringResource(R.string.reset_message)) },
                            confirmButton = {
                                TextButton(onClick = { 
                                    prefs.reset()
                                    LocalLearningStore(context).clear()
                                    ClipboardHistoryStore(context).clear()
                                    TouchPersonalizationStore(context).reset()
                                    onThemeChanged(prefs.theme)
                                    refresh++
                                    showDialog = null 
                                }) { Text(stringResource(R.string.clear)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = null }) { Text(stringResource(android.R.string.cancel)) }
                            }
                        )
                    }

                    SettingsActionRow(
                        title = stringResource(R.string.clear_learning_title),
                        summary = stringResource(R.string.clear_learning_summary),
                        icon = Icons.Default.Delete,
                        onClick = { showDialog = "clear_learning" }
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.reset_touch_title),
                        summary = stringResource(R.string.reset_touch_summary),
                        icon = Icons.Default.TouchApp,
                        onClick = { showDialog = "reset_touch" }
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.clear_clipboard_title),
                        summary = stringResource(R.string.clear_clipboard_summary),
                        icon = Icons.Default.ClearAll,
                        onClick = { showDialog = "clear_clipboard" }
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.reset_title),
                        summary = stringResource(R.string.reset_summary),
                        icon = Icons.Default.Restore,
                        onClick = { showDialog = "reset_all" }
                    )
                }
            }

            item {
                AccordionSection("About", Icons.Default.Info, expanded = expandedSection == "About", onExpandedChange = { expandedSection = if (it) "About" else null }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Made in ❤️ with Sri Lanka",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Developed by Dinush Lakmal\nEmail: dinushlakmal01@gmail.com",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.5f).padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Slashboard v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 24.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            shape = CircleShape,
            shadowElevation = 8.dp,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_slashboard_logo),
                contentDescription = "Slashboard Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            visible = true
        }
        AnimatedVisibility(visible = visible) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Slashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AccordionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onExpandedChange(!expanded) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsChoiceRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    entries: List<String>,
    values: List<String>,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentIndex = values.indexOf(currentValue).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = entries.getOrElse(currentIndex) { "" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entries.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onValueChange(values[index])
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    summary: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    statusBadge: String? = null,
    isCompleted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (isCompleted) MaterialTheme.colorScheme.primary else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (statusBadge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = statusBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun intColor(c: Int): Color = Color(c.toLong() and 0xFFFFFFFFL)

@Composable
fun InteractiveKeyboardPreview(prefs: KeyboardPreferences, refresh: Int) {
    var testText by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    val dummyActions = object : org.slashboard.ime.ime.KeyboardActions {
                        override fun onCharacter(value: String) { testText += value }
                        override fun onBackspace(word: Boolean) { 
                            if (testText.isNotEmpty()) testText = testText.dropLast(1)
                        }
                        override fun onSpace() { testText += " " }
                        override fun onEnter() { testText += "\n" }
                        override fun onCandidate(value: String) {}
                        override fun onGlobe() {}
                        override fun onModeRequested(mode: org.slashboard.ime.engine.InputMode) {}
                        override fun onHide() {}
                        override fun onCursorDelta(delta: Int) {}
                    }
                    // For the settings preview, disable emoji repo and clipboard store to speed up init
                    val kv = org.slashboard.ime.ime.KeyboardView(ctx, dummyActions, prefs)
                    kv.configure(org.slashboard.ime.engine.InputMode.SMART_PHONETIC, false, "↵")
                    kv
                },
                update = { view: org.slashboard.ime.ime.KeyboardView ->
                    // Re-apply theme when it changes
                    refresh.hashCode() // read the state to trigger recomposition
                    view.applyTheme()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            label = { Text("Tap on the preview above to test") },
            placeholder = { Text("Type here using the preview keyboard...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            singleLine = true,
            readOnly = true,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeLayoutsScreen(
    prefs: KeyboardPreferences,
    onThemeChanged: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val themeValues = remember {
        listOf(
            "system", "light", "dark", "ocean_blue", "forest_green", "sunset",
            "cyberpunk", "dracula", "nord", "monokai", "lavender", "rose_gold",
            "midnight", "neon_green", "cherry", "coffee", "deep_space", "mint",
            "crimson", "solarized_dark", "solarized_light", "matcha", "coral",
            "peach", "royal_purple", "gold", "silver", "emerald", "ruby",
            "sapphire", "amethyst", "aquamarine", "obsidian"
        )
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Layouts", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            InteractiveKeyboardPreview(prefs = prefs, refresh = refresh)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 6.dp,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(themeValues) { themeValue ->
                    val palette = remember(themeValue, prefs.highContrast, refresh) {
                        KeyboardPaletteResolver.resolve(context, themeValue, prefs.highContrast)
                    }
                    val isSelected = prefs.theme == themeValue

                    MiniKeyboardPreview(
                        palette = palette,
                        isSelected = isSelected,
                        onClick = {
                            prefs.theme = themeValue
                            onThemeChanged(themeValue)
                            refresh++
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MiniKeyboardPreview(
    palette: KeyboardPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = when {
        isPressed -> 1.02f
        isSelected -> 1.045f
        isHovered -> 1.035f
        else -> 1.0f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tile_scale"
    )

    val targetElevation = when {
        isSelected -> 8.dp
        isHovered -> 6.dp
        else -> 2.dp
    }
    val animatedElevation by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = tween(durationMillis = 200),
        label = "tile_elevation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = intColor(palette.background)),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .scale(animatedScale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .drawBehind {
                    val keyColor = Color(palette.key.toLong() and 0xFFFFFFFFL)
                    val utilColor = Color(palette.utility.toLong() and 0xFFFFFFFFL)
                    val actionColor = Color(palette.action.toLong() and 0xFFFFFFFFL)
                    val actionTextColor = Color(palette.actionText.toLong() and 0xFFFFFFFFL)
                    val inkColor = Color(palette.ink.toLong() and 0xFFFFFFFFL)

                    val spacing = 2.dp.toPx()
                    val radius = CornerRadius(3.dp.toPx())
                    val dotRadius = 1.5.dp.toPx()

                    val contentWidth = size.width
                    
                    // Vertical arrangement computation
                    val rowHeight = 14.dp.toPx()
                    val barHeight = 4.dp.toPx()
                    // Total height needed for content = barHeight + 4 * rowHeight
                    val totalContentHeight = barHeight + (4 * rowHeight)
                    val availableVerticalSpace = size.height - totalContentHeight
                    val rowSpacing = availableVerticalSpace / 4f

                    var currentY = 0f

                    // Suggestion bar indicator (3 items: 1f, 1.5f, 1f weight)
                    val barSpacing = 4.dp.toPx()
                    val barPad = 4.dp.toPx()
                    val barAvailableWidth = contentWidth - 2 * barPad - 2 * barSpacing
                    val barUnit = barAvailableWidth / 3.5f
                    
                    var barX = barPad
                    // Item 1
                    drawRoundRect(color = inkColor.copy(alpha = 0.25f), topLeft = Offset(barX, currentY + 0.5.dp.toPx()), size = Size(barUnit, 3.dp.toPx()), cornerRadius = CornerRadius(1.5.dp.toPx()))
                    barX += barUnit + barSpacing
                    // Item 2
                    drawRoundRect(color = inkColor.copy(alpha = 0.45f), topLeft = Offset(barX, currentY), size = Size(1.5f * barUnit, 4.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
                    barX += 1.5f * barUnit + barSpacing
                    // Item 3
                    drawRoundRect(color = inkColor.copy(alpha = 0.25f), topLeft = Offset(barX, currentY + 0.5.dp.toPx()), size = Size(barUnit, 3.dp.toPx()), cornerRadius = CornerRadius(1.5.dp.toPx()))
                    
                    currentY += barHeight + rowSpacing

                    // Row 1: 10 keys
                    val keyWidthR1 = (contentWidth - 9 * spacing) / 10f
                    for (i in 0 until 10) {
                        val kX = i * (keyWidthR1 + spacing)
                        drawRoundRect(color = keyColor, topLeft = Offset(kX, currentY), size = Size(keyWidthR1, rowHeight), cornerRadius = radius)
                        drawCircle(color = inkColor.copy(alpha = 0.7f), radius = dotRadius, center = Offset(kX + keyWidthR1 / 2f, currentY + rowHeight / 2f))
                    }

                    currentY += rowHeight + rowSpacing

                    // Row 2: 9 keys
                    val r2Padding = 4.dp.toPx()
                    val r2Width = contentWidth - 2 * r2Padding
                    val keyWidthR2 = (r2Width - 8 * spacing) / 9f
                    for (i in 0 until 9) {
                        val kX = r2Padding + i * (keyWidthR2 + spacing)
                        drawRoundRect(color = keyColor, topLeft = Offset(kX, currentY), size = Size(keyWidthR2, rowHeight), cornerRadius = radius)
                        drawCircle(color = inkColor.copy(alpha = 0.7f), radius = dotRadius, center = Offset(kX + keyWidthR2 / 2f, currentY + rowHeight / 2f))
                    }

                    currentY += rowHeight + rowSpacing

                    // Row 3: Shift (1.4f), 7 keys (1f), Backspace (1.4f)
                    val totalWeightR3 = 1.4f + 7f + 1.4f
                    val availableWidthR3 = contentWidth - 8 * spacing
                    val unitWidthR3 = availableWidthR3 / totalWeightR3

                    // Shift
                    val shiftWidth = 1.4f * unitWidthR3
                    drawRoundRect(color = utilColor, topLeft = Offset(0f, currentY), size = Size(shiftWidth, rowHeight), cornerRadius = radius)
                    // Draw little arrow for shift
                    val shiftCenter = Offset(shiftWidth / 2f, currentY + rowHeight / 2f)
                    drawLine(color = inkColor, start = Offset(shiftCenter.x, shiftCenter.y + 2.dp.toPx()), end = Offset(shiftCenter.x, shiftCenter.y - 3.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = inkColor, start = Offset(shiftCenter.x - 2.dp.toPx(), shiftCenter.y - 1.dp.toPx()), end = Offset(shiftCenter.x, shiftCenter.y - 3.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = inkColor, start = Offset(shiftCenter.x + 2.dp.toPx(), shiftCenter.y - 1.dp.toPx()), end = Offset(shiftCenter.x, shiftCenter.y - 3.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)

                    var currentX = shiftWidth + spacing
                    // 7 Keys
                    for (i in 0 until 7) {
                        drawRoundRect(color = keyColor, topLeft = Offset(currentX, currentY), size = Size(unitWidthR3, rowHeight), cornerRadius = radius)
                        drawCircle(color = inkColor.copy(alpha = 0.7f), radius = dotRadius, center = Offset(currentX + unitWidthR3 / 2f, currentY + rowHeight / 2f))
                        currentX += unitWidthR3 + spacing
                    }

                    // Backspace
                    val backspaceWidth = 1.4f * unitWidthR3
                    drawRoundRect(color = utilColor, topLeft = Offset(currentX, currentY), size = Size(backspaceWidth, rowHeight), cornerRadius = radius)
                    val bsCenter = Offset(currentX + backspaceWidth / 2f, currentY + rowHeight / 2f)
                    drawLine(color = inkColor, start = Offset(bsCenter.x + 2.dp.toPx(), bsCenter.y), end = Offset(bsCenter.x - 3.dp.toPx(), bsCenter.y), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = inkColor, start = Offset(bsCenter.x - 1.dp.toPx(), bsCenter.y - 2.dp.toPx()), end = Offset(bsCenter.x - 3.dp.toPx(), bsCenter.y), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = inkColor, start = Offset(bsCenter.x - 1.dp.toPx(), bsCenter.y + 2.dp.toPx()), end = Offset(bsCenter.x - 3.dp.toPx(), bsCenter.y), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = inkColor, start = Offset(bsCenter.x, bsCenter.y - 2.dp.toPx()), end = Offset(bsCenter.x + 2.dp.toPx(), bsCenter.y + 2.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = inkColor, start = Offset(bsCenter.x, bsCenter.y + 2.dp.toPx()), end = Offset(bsCenter.x + 2.dp.toPx(), bsCenter.y - 2.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)

                    currentY += rowHeight + rowSpacing

                    // Row 4: 123 (1.4f), Space (4.6f), Enter (1.8f)
                    val totalWeightR4 = 1.4f + 4.6f + 1.8f
                    val availableWidthR4 = contentWidth - 2 * spacing
                    val unitWidthR4 = availableWidthR4 / totalWeightR4

                    currentX = 0f
                    // 123
                    val symWidth = 1.4f * unitWidthR4
                    drawRoundRect(color = utilColor, topLeft = Offset(currentX, currentY), size = Size(symWidth, rowHeight), cornerRadius = radius)
                    drawRoundRect(color = inkColor.copy(alpha = 0.7f), topLeft = Offset(currentX + symWidth / 2f - 4.dp.toPx(), currentY + rowHeight / 2f - 1.dp.toPx()), size = Size(8.dp.toPx(), 2.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
                    currentX += symWidth + spacing

                    // Space
                    val spaceWidth = 4.6f * unitWidthR4
                    drawRoundRect(color = keyColor, topLeft = Offset(currentX, currentY), size = Size(spaceWidth, rowHeight), cornerRadius = radius)
                    drawRoundRect(color = inkColor.copy(alpha = 0.4f), topLeft = Offset(currentX + spaceWidth / 2f - 11.dp.toPx(), currentY + rowHeight / 2f - 1.dp.toPx()), size = Size(22.dp.toPx(), 2.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()))
                    currentX += spaceWidth + spacing

                    // Enter
                    val enterWidth = 1.8f * unitWidthR4
                    drawRoundRect(color = actionColor, topLeft = Offset(currentX, currentY), size = Size(enterWidth, rowHeight), cornerRadius = radius)
                    val enterCenter = Offset(currentX + enterWidth / 2f, currentY + rowHeight / 2f)
                    drawLine(color = actionTextColor, start = Offset(enterCenter.x + 2.dp.toPx(), enterCenter.y - 2.dp.toPx()), end = Offset(enterCenter.x + 2.dp.toPx(), enterCenter.y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = actionTextColor, start = Offset(enterCenter.x + 2.dp.toPx(), enterCenter.y + 1.dp.toPx()), end = Offset(enterCenter.x - 2.dp.toPx(), enterCenter.y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = actionTextColor, start = Offset(enterCenter.x, enterCenter.y - 1.dp.toPx()), end = Offset(enterCenter.x - 2.dp.toPx(), enterCenter.y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = actionTextColor, start = Offset(enterCenter.x, enterCenter.y + 3.dp.toPx()), end = Offset(enterCenter.x - 2.dp.toPx(), enterCenter.y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                }
        ) {
            // Selection indicator badge
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
