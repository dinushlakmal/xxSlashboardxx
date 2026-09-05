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
                        title = stringResource(R.string.use_english),
                        summary = stringResource(R.string.use_english_summary),
                        icon = Icons.Default.GTranslate,
                        checked = prefs.useEnglish,
                        onCheckedChange = { prefs.useEnglish = it; refresh++ }
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
                AccordionSection("Appearance", Icons.Default.Palette, expanded = expandedSection == "Appearance", onExpandedChange = { expandedSection = if (it) "Appearance" else null }) {
                    SettingsChoiceRow(
                        title = stringResource(R.string.theme),
                        icon = Icons.Default.Palette,
                        entries = stringArrayResource(R.array.theme_entries).toList(),
                        values = stringArrayResource(R.array.theme_values).toList(),
                        currentValue = prefs.theme,
                        onValueChange = { selected ->
                            originalTheme = prefs.theme
                            prefs.theme = selected
                            previewTheme = selected
                            refresh++
                        }
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
                        checked = true,
                        onCheckedChange = {}
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
                                    context.getSharedPreferences("slashboard_touch_model", 0).edit().clear().apply()
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
                                    // Handle clear clipboard
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
                                    context.getSharedPreferences("slashboard_touch_model", 0).edit().clear().apply()
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
                    }
                }
            }
        }
    }

    if (previewTheme != null) {
        ThemePreviewSheet(
            previewTheme = previewTheme!!,
            onApply = {
                previewTheme = null
                originalTheme = null
                refresh++
            },
            onDismiss = {
                prefs.theme = originalTheme!!
                previewTheme = null
                originalTheme = null
                refresh++
            },
            prefs = prefs
        )
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
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Slashboard Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            visible = true
        }
        AnimatedVisibility(visible = visible) {
            Text(
                text = "Slashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePreviewSheet(
    previewTheme: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    prefs: KeyboardPreferences
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Theme Preview",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            AndroidView(
                factory = { ctx ->
                    val actions = object : KeyboardActions {
                        override fun onCharacter(value: String) {}
                        override fun onBackspace(word: Boolean) {}
                        override fun onSpace() {}
                        override fun onEnter() {}
                        override fun onCandidate(value: String) {}
                        override fun onGlobe() {}
                        override fun onModeRequested(mode: InputMode) {}
                        override fun onHide() {}
                        override fun onCursorDelta(delta: Int) {}
                        override fun languageScoreForKey(output: String): Float = 0f
                    }
                    KeyboardView(ctx, actions, prefs).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApply) { Text("Apply") }
            }
        }
    }
}
