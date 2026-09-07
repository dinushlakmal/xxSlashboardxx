with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'r') as f:
    text = f.read()

# Simply find ALL occurrences of 'import' that are directly attached to previous words
text = text.replace('settingsimport', 'settings\nimport')
text = text.replace('Editimport', 'Edit\nimport')
text = text.replace('Activityimport', 'Activity\nimport')
text = text.replace('ComponentNameimport', 'ComponentName\nimport')
text = text.replace('Intentimport', 'Intent\nimport')
text = text.replace('Bundleimport', 'Bundle\nimport')
text = text.replace('Settingsimport', 'Settings\nimport')
text = text.replace('InputMethodManagerimport', 'InputMethodManager\nimport')
text = text.replace('ComponentActivityimport', 'ComponentActivity\nimport')
text = text.replace('BackHandlerimport', 'BackHandler\nimport')
text = text.replace('setContentimport', 'setContent\nimport')
text = text.replace('AnimatedVisibilityimport', 'AnimatedVisibility\nimport')
text = text.replace('FastOutSlowInEasingimport', 'FastOutSlowInEasing\nimport')
text = text.replace('LinearOutSlowInEasingimport', 'LinearOutSlowInEasing\nimport')
text = text.replace('RepeatModeimport', 'RepeatMode\nimport')
text = text.replace('animateFloatimport', 'animateFloat\nimport')
text = text.replace('infiniteRepeatableimport', 'infiniteRepeatable\nimport')
text = text.replace('rememberInfiniteTransitionimport', 'rememberInfiniteTransition\nimport')
text = text.replace('animateDpAsStateimport', 'animateDpAsState\nimport')
text = text.replace('animateFloatAsStateimport', 'animateFloatAsState\nimport')
text = text.replace('springimport', 'spring\nimport')
text = text.replace('Springimport', 'Spring\nimport')
text = text.replace('tweenimport', 'tween\nimport')
text = text.replace('expandVerticallyimport', 'expandVertically\nimport')

# Also handle standard cases where we just need to add a newline before `import ` if missing
import re
text = re.sub(r'([A-Za-z0-9_])import\s+(android|androidx|org|java)\b', r'\1\nimport \2', text)
text = re.sub(r'([A-Za-z0-9_])import\s+kotlin\b', r'\1\nimport kotlin', text)

with open('app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt', 'w') as f:
    f.write(text)
