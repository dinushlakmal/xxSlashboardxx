with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'r') as f:
    content = f.read()

prefix = """package org.slashboard.ime.settings
import androidx.compose.material.icons.filled.Edit
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
"""

# The corrupted part starts with 'package org.slashboard.ime.settingsimport' and ends with 'AnimatedVisibility'
import re
# We'll just replace the exact literal string we see
bad_string = "package org.slashboard.ime.settingsimport androidx.compose.material.icons.filled.Editimport android.app.Activityimport android.content.ComponentNameimport android.content.Intentimport android.os.Bundleimport android.provider.Settingsimport android.view.inputmethod.InputMethodManagerimport androidx.activity.ComponentActivityimport androidx.activity.compose.BackHandlerimport androidx.activity.compose.setContentimport androidx.compose.animation.AnimatedVisibility"

new_content = content.replace(bad_string, prefix)

with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'w') as f:
    f.write(new_content)
