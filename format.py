import re
with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'r') as f:
    text = f.read()

# find the package statement and imports block, which might be messed up
# we know it starts with 'package org.slashboard.ime.settings'
# Let's extract everything up to 'class SettingsActivity'
match = re.search(r'(package org\.slashboard\.ime\.settings.*?)(class SettingsActivity)', text, flags=re.DOTALL)
if match:
    imports_block = match.group(1)
    # clean it up
    # remove all newlines
    imports_block = imports_block.replace('\n', '')
    # replace 'import' with '\nimport '
    imports_block = imports_block.replace('import', '\nimport ')
    # replace 'package org.slashboard.ime.settings' with 'package org.slashboard.ime.settings\n'
    imports_block = imports_block.replace('package org.slashboard.ime.settings\nimport ', 'package org.slashboard.ime.settings\nimport ')
    
    # but wait, 'import' without space is now '\nimport '.
    # Let's just fix it perfectly:
    import_list = [i.strip() for i in imports_block.split('\nimport ') if i.strip()]
    
    # first element might be 'package org.slashboard.ime.settings'
    first = import_list[0]
    first = first.replace('package org.slashboard.ime.settings', 'package org.slashboard.ime.settings\n')
    
    clean_imports = first + '\n' + '\n'.join(['import ' + i for i in import_list[1:]]) + '\n\n'
    
    new_text = text[:match.start()] + clean_imports + text[match.end(1):]
    with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'w') as f:
        f.write(new_text)
