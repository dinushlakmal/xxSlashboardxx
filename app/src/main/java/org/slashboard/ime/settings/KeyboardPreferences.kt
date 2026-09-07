package org.slashboard.ime.settings

import android.content.Context
import org.slashboard.ime.engine.InputMode

class KeyboardPreferences(context: Context) {
    val store = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    var mode: InputMode
        get() = runCatching { InputMode.valueOf(store.getString(MODE, null) ?: "SMART_PHONETIC") }.getOrDefault(InputMode.SMART_PHONETIC)
        set(value) = store.edit().putString(MODE, value.name).apply()
    var useEnglish: Boolean by bool("use_english", false)
    var suggestions: Boolean by bool(SUGGESTIONS, true)
    var emojiSuggestions: Boolean by bool(EMOJI_SUGGESTIONS, false)
    var emojiPicker: Boolean by bool(EMOJI_PICKER, true)
    var haptics: Boolean by bool(HAPTICS, true)
    var keySounds: Boolean by bool(KEY_SOUNDS, false)
    var soundPack: String
        get() = store.getString(SOUND_PACK, "system") ?: "system"
        set(value) = store.edit().putString(SOUND_PACK, value).apply()
    var highContrast: Boolean by bool(HIGH_CONTRAST, false)
    var clipboardHistory: Boolean by bool(CLIPBOARD, false)
    var topRow: String
        get() = store.getString(TOP_ROW, "none") ?: "none"
        set(value) = store.edit().putString(TOP_ROW, value).apply()
    var oneHanded: String
        get() = store.getString(ONE_HANDED, "center") ?: "center"
        set(value) = store.edit().putString(ONE_HANDED, value).apply()
    var keySpacing: String
        get() = store.getString(KEY_SPACING, "standard") ?: "standard"
        set(value) = store.edit().putString(KEY_SPACING, value).apply()
    var keyboardSize: String
        get() = store.getString(KEYBOARD_SIZE, "standard") ?: "standard"
        set(value) = store.edit().putString(KEYBOARD_SIZE, value).apply()
    var spatialDecoder: Boolean by bool(SPATIAL_DECODER, true)
    var debugOverlay: Boolean by bool(DEBUG_OVERLAY, false)
    var theme: String
        get() = store.getString(THEME, "dark") ?: "dark"
        set(value) = store.edit().putString(THEME, value).apply()
    var skinTone: String
        get() = store.getString(SKIN_TONE, "") ?: ""
        set(value) = store.edit().putString(SKIN_TONE, value).apply()
    var toolbarIcons: String
        get() = store.getString(TOOLBAR_ICONS, DEFAULT_TOOLBAR_ICONS) ?: DEFAULT_TOOLBAR_ICONS
        set(value) = store.edit().putString(TOOLBAR_ICONS, value).apply()
    var longPressMs: Long
        get() = store.getLong(LONG_PRESS_MS, 400L).coerceIn(150L, 500L)
        set(value) = store.edit().putLong(LONG_PRESS_MS, value.coerceIn(150L, 500L)).apply()
    var appLayoutMemory: Boolean by bool(APP_LAYOUT_MEMORY, true)
    var smartKeyModifiers: Boolean by bool(SMART_KEY_MODIFIERS, true)
    var thumbReachMode: String
        get() = store.getString(THUMB_REACH_MODE, "off") ?: "off"
        set(value) = store.edit().putString(THUMB_REACH_MODE, value).apply()

    var customSpacebarText: String
        get() = store.getString(CUSTOM_SPACEBAR_TEXT, "") ?: ""
        set(value) = store.edit().putString(CUSTOM_SPACEBAR_TEXT, value).apply()

    var confirmExit: Boolean by bool(CONFIRM_EXIT, true)
    var firstLaunchPermissionsPrompted: Boolean by bool(FIRST_LAUNCH_PERMS, false)

    var recentEmojis: List<String>
        get() {
            val raw = store.getString("recent_emojis", null)
            if (raw.isNullOrEmpty()) {
                return listOf("😂", "❤️", "🙏", "👍", "😍", "🤣", "😊", "🔥", "🥺", "🥰", "😭", "✨", "🎉", "👏", "😁", "😘", "😎", "😅")
            }
            return raw.split(",").filter { it.isNotEmpty() }
        }
        set(value) = store.edit().putString("recent_emojis", value.joinToString(",")).apply()

    fun isToolbarIconEnabled(id: String): Boolean {
        val list = toolbarIcons.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return id in list
    }

    fun setToolbarIconEnabled(id: String, enabled: Boolean) {
        val current = toolbarIcons.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (enabled) {
            if (id !in current) {
                if (id == "lang_toggle") {
                    current.add(0, id)
                } else if (current.isNotEmpty() && current.last() == "settings") {
                    current.add(current.size - 1, id)
                } else {
                    current.add(id)
                }
            }
        } else {
            current.remove(id)
        }
        toolbarIcons = current.joinToString(",")
    }

    fun moveToolbarIcon(id: String, moveUp: Boolean) {
        val current = toolbarIcons.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val index = current.indexOf(id)
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex in 0 until current.size) {
            val item = current.removeAt(index)
            current.add(targetIndex, item)
            toolbarIcons = current.joinToString(",")
        }
    }

    fun swapToolbarIcons(indexA: Int, indexB: Int) {
        val current = toolbarIcons.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (indexA in current.indices && indexB in current.indices && indexA != indexB) {
            val temp = current[indexA]
            current[indexA] = current[indexB]
            current[indexB] = temp
            toolbarIcons = current.joinToString(",")
        }
    }

    fun reorderToolbarIcons(newList: List<String>) {
        toolbarIcons = newList.filter { it.isNotEmpty() }.joinToString(",")
    }

    fun resetToolbarIcons() {
        toolbarIcons = DEFAULT_TOOLBAR_ICONS
    }

    fun reset() { store.edit().clear().apply() }
    private fun bool(key: String, default: Boolean) = object : kotlin.properties.ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = store.getBoolean(key, default)
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Boolean) { store.edit().putBoolean(key, value).apply() }
    }
    companion object {
        const val FILE = "slashboard_keyboard_preferences"
        private const val MODE = "mode"; private const val SUGGESTIONS = "suggestions"
        private const val EMOJI_SUGGESTIONS = "emoji_suggestions"; private const val EMOJI_PICKER = "emoji_picker"
        private const val HAPTICS = "haptics"; private const val KEY_SOUNDS = "key_sounds"
        private const val SOUND_PACK = "sound_pack"
        private const val HIGH_CONTRAST = "high_contrast"; private const val CLIPBOARD = "clipboard"
        private const val TOP_ROW = "top_row"; private const val ONE_HANDED = "one_handed"
        private const val KEY_SPACING = "key_spacing"; private const val KEYBOARD_SIZE = "keyboard_size"
        private const val SPATIAL_DECODER = "spatial_decoder"; private const val DEBUG_OVERLAY = "debug_overlay"
        private const val THEME = "theme"; private const val SKIN_TONE = "skin_tone"
        private const val TOOLBAR_ICONS = "toolbar_icons"
        private const val LONG_PRESS_MS = "long_press_ms"
        private const val APP_LAYOUT_MEMORY = "app_layout_memory"
        private const val SMART_KEY_MODIFIERS = "smart_key_modifiers"
        private const val THUMB_REACH_MODE = "thumb_reach_mode"
        private const val CUSTOM_SPACEBAR_TEXT = "custom_spacebar_text"
        private const val CONFIRM_EXIT = "confirm_exit"
        private const val FIRST_LAUNCH_PERMS = "first_launch_perms"
        const val DEFAULT_TOOLBAR_ICONS = "lang_toggle,undo,redo,astrology,fm,translate,emoji,clipboard,settings"
    }
}
