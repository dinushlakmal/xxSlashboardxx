import re

with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'r') as f:
    text = f.read()

replacement = """                    var showSpacebarDialog by remember { mutableStateOf(false) }
                    SettingsActionRow(
                        title = "Spacebar Name",
                        summary = if (prefs.customSpacebarText.isEmpty()) "Default" else prefs.customSpacebarText,
                        icon = Icons.Default.SpaceBar,
                        onClick = { showSpacebarDialog = true }
                    )
                    
                    if (showSpacebarDialog) {
                        var textValue by remember { mutableStateOf(prefs.customSpacebarText) }
                        AlertDialog(
                            onDismissRequest = { showSpacebarDialog = false },
                            title = { Text("Custom Spacebar Name") },
                            text = {
                                OutlinedTextField(
                                    value = textValue,
                                    onValueChange = { if (it.length <= 12) textValue = it },
                                    label = { Text("Name (max 12)") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { 
                                    prefs.customSpacebarText = textValue
                                    refresh++
                                    showSpacebarDialog = false
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSpacebarDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    SettingsChoiceRow("""

text = re.sub(r'                    SettingsChoiceRow\(', replacement, text, count=1)

with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'w') as f:
    f.write(text)

