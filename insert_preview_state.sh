sed -i '/var expandedSection by remember { mutableStateOf<String?>("Setup") }/a \
    var previewTheme by remember { mutableStateOf<String?>(null) }\
    var originalTheme by remember { mutableStateOf<String?>(null) }' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
