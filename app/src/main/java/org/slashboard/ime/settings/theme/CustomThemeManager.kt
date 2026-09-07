package org.slashboard.ime.settings.theme

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.InputStream

data class CustomThemeData(
    val id: String,
    val name: String,
    val background: String,
    val key: String,
    val utility: String,
    val ink: String,
    val action: String,
    val actionText: String,
    val selected: String,
    val dark: Boolean,
    val highContrast: Boolean,
    val keyRadiusDp: Float? = null,
    val blurEffect: Boolean = false,
    val keyOpacity: Float = 1.0f,
    val backgroundImagePath: String? = null,
    val spaceKey: String? = null,
    val spaceBorder: String? = null
)

object CustomThemeManager {
    fun getThemesDir(context: Context): File {
        val dir = File(context.filesDir, "themes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getThemes(context: Context): List<CustomThemeData> {
        val dir = getThemesDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".json") || name.endsWith(".slashtheme") } ?: return emptyList()
        val themes = mutableListOf<CustomThemeData>()
        for (file in files) {
            try {
                val json = JSONObject(file.readText())
                themes.add(parseTheme(file.nameWithoutExtension, json))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return themes
    }

    fun parseTheme(id: String, json: JSONObject): CustomThemeData {
        return CustomThemeData(
            id = id,
            name = json.optString("name", id),
            background = json.optString("background", "#0F172A"),
            key = json.optString("key", "#1E293B"),
            utility = json.optString("utility", "#1E293B"),
            ink = json.optString("ink", "#38BDF8"),
            action = json.optString("action", "#0284C7"),
            actionText = json.optString("actionText", "#FFFFFF"),
            selected = json.optString("selected", "#334155"),
            dark = json.optBoolean("dark", true),
            highContrast = json.optBoolean("highContrast", false),
            keyRadiusDp = if (json.has("keyRadiusDp")) json.optDouble("keyRadiusDp").toFloat() else null,
            blurEffect = json.optBoolean("blurEffect", false),
            keyOpacity = json.optDouble("keyOpacity", 1.0).toFloat(),
            backgroundImagePath = json.optString("backgroundImagePath").takeIf { it.isNotEmpty() },
            spaceKey = json.optString("spaceKey").takeIf { it.isNotEmpty() },
            spaceBorder = json.optString("spaceBorder").takeIf { it.isNotEmpty() }
        )
    }

    fun importTheme(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(content)
                val id = "custom_" + System.currentTimeMillis()
                val file = File(getThemesDir(context), "$id.slashtheme")
                file.writeText(content)
                id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getTheme(context: Context, id: String): CustomThemeData? {
        val file = File(getThemesDir(context), "$id.slashtheme")
        val fileJson = File(getThemesDir(context), "$id.json")
        val target = if (file.exists()) file else if (fileJson.exists()) fileJson else return null
        
        return try {
            val json = JSONObject(target.readText())
            parseTheme(id, json)
        } catch (e: Exception) {
            null
        }
    }
}
