sed -i '/override fun onBindInput() {/,/precedingDirty = true/d' app/src/main/java/org/slashboard/ime/ime/SlashboardInputMethodService.kt
sed -i '/override fun onUnbindInput() {/,/precedingDirty = true/d' app/src/main/java/org/slashboard/ime/ime/SlashboardInputMethodService.kt
sed -i '/    }/,/    }/d' app/src/main/java/org/slashboard/ime/ime/SlashboardInputMethodService.kt
