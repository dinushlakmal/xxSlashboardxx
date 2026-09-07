import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    text = f.read()

replacement = """        <activity
            android:name="org.slashboard.ime.settings.PermissionActivity"
            android:theme="@android:style/Theme.Translucent.NoTitleBar"
            android:exported="false" />
        <activity"""

text = text.replace('        <activity', replacement, 1)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(text)
