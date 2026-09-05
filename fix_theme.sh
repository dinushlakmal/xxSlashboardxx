awk '
/SettingsChoiceRow\(/ {
    in_row=1
    buf=$0
    next
}
in_row {
    buf=buf"\n"$0
    if (/onValueChange = \{ prefs.theme = it; refresh\+\+ \}/) {
        print "                    SettingsChoiceRow("
        print "                        title = stringResource(R.string.theme),"
        print "                        icon = Icons.Default.Palette,"
        print "                        entries = stringArrayResource(R.array.theme_entries).toList(),"
        print "                        values = stringArrayResource(R.array.theme_values).toList(),"
        print "                        currentValue = prefs.theme,"
        print "                        onValueChange = { selected ->"
        print "                            originalTheme = prefs.theme"
        print "                            prefs.theme = selected"
        print "                            previewTheme = selected"
        print "                            refresh++"
        print "                        }"
        in_row=0
        next
    }
    if (/\)/ && !/it; refresh\+\+/) {
        # This is a different SettingsChoiceRow, dump it
        print buf
        in_row=0
        next
    }
    next
}
{ print }
' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt > app/src/main/java/org/slashboard/ime/settings/SettingsActivity_new.kt
mv app/src/main/java/org/slashboard/ime/settings/SettingsActivity_new.kt app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
