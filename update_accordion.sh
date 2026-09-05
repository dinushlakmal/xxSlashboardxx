sed -i 's/    initiallyExpanded: Boolean = false,/    expanded: Boolean,\n    onExpandedChange: (Boolean) -> Unit,/g' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
sed -i 's/    var expanded by remember { mutableStateOf(initiallyExpanded) }//g' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
sed -i 's/.clickable { expanded = !expanded }/.clickable { onExpandedChange(!expanded) }/g' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
