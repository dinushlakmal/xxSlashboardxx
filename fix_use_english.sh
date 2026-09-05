awk '
/SettingsToggleRow\(/ {
    in_row=1
    buf=$0
    is_use_english=0
    next
}
in_row {
    buf=buf"\n"$0
    if (/use_english/) {
        is_use_english=1
    }
    if (/\)/ && !/it; refresh\+\+/) {
        if (is_use_english) {
            # Skip printing
            in_row=0
            next
        } else {
            print buf
            in_row=0
            next
        }
    }
    if (/\)/ && /it; refresh\+\+/) {
        if (is_use_english) {
            in_row=0
            next
        } else {
            print buf
            in_row=0
            next
        }
    }
    if (/\)/ && !/Icons.Default/) {
        # Check if it was empty SettingsToggleRow()
        if (buf ~ /SettingsToggleRow\(\s*\n*\s*\)/) {
            in_row = 0
            next
        }
    }
    next
}
{ print }
' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt > app/src/main/java/org/slashboard/ime/settings/SettingsActivity_new.kt
mv app/src/main/java/org/slashboard/ime/settings/SettingsActivity_new.kt app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
