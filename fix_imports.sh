awk '
/^import org.slashboard.ime.engine.InputMode$/ {
    if (seen) {
        next
    }
    seen = 1
}
{ print }
' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt > app/src/main/java/org/slashboard/ime/settings/SettingsActivity_new.kt
mv app/src/main/java/org/slashboard/ime/settings/SettingsActivity_new.kt app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
