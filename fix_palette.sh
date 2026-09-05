sed -i 's/return if (theme == "system" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {/return fixed(dark, highContrast) \/* /g' app/src/main/java/org/slashboard/ime/ime/KeyboardPalette.kt
sed -i 's/        } else {/        *\/ if (false) {/g' app/src/main/java/org/slashboard/ime/ime/KeyboardPalette.kt
