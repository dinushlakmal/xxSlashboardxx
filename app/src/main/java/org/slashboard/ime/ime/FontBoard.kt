package org.slashboard.ime.ime

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.slashboard.ime.R
import org.slashboard.ime.settings.KeyboardPreferences
import org.slashboard.ime.settings.SettingsActivity
import org.slashboard.ime.settings.font.CustomFontManager
import org.slashboard.ime.settings.font.FontItem

/**
 * In-Keyboard Font Studio Layout Panel.
 * Displays font styles in a compact, vertically scrollable grid (3 or 4 per row),
 * allowing users to immediately preview and activate any font style directly from the keyboard.
 * The Default font style is always positioned first at index 0.
 */
internal class FontBoard(
    context: Context,
    private val colors: KeyboardColors,
    private val prefs: KeyboardPreferences,
    private val onFontSelected: (String) -> Unit,
    private val onDismiss: () -> Unit
) : LinearLayout(context) {

    private val dp = { value: Number ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }

    // High-Contrast Dark & Adaptive Palette
    private val fbBg = if (colors.dark) Color.parseColor("#121212") else Color.parseColor("#F5F5F7")
    private val fbCardBg = if (colors.dark) Color.parseColor("#1E1E1E") else Color.parseColor("#FFFFFF")
    private val fbCardStroke = if (colors.dark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
    private val fbActiveStroke = colors.action
    private val fbActiveBg = if (colors.dark) ColorUtils.setAlphaComponent(colors.action, 45) else ColorUtils.setAlphaComponent(colors.action, 28)
    private val fbText = colors.ink
    private val fbMutedText = if (colors.dark) Color.parseColor("#9E9E9E") else Color.parseColor("#666666")
    private val fbChipBg = if (colors.dark) Color.parseColor("#252525") else Color.parseColor("#EAEAEA")

    private val allFonts: List<FontItem> = CustomFontManager.getAllFonts(context)
    private var selectedCategory: String = "All"
    private var displayedFonts: List<FontItem> = allFonts

    private val activeFontBadge = TextView(context)
    private val categoryScroll = HorizontalScrollView(context)
    private val categoryRow = LinearLayout(context)
    private val fontGrid = RecyclerView(context)
    private lateinit var fontAdapter: FontGridAdapter

    init {
        orientation = VERTICAL
        setBackgroundColor(fbBg)
        clipChildren = true
        clipToPadding = true

        buildHeader()
        buildCategoryChips()
        buildFontGrid()
    }

    private fun buildHeader() {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        // Back / Close Button to return to standard typing
        val backBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_key_back)
            imageTintList = ColorStateList.valueOf(fbText)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(6), dp(6), dp(6), dp(6))
            contentDescription = "Back to Keyboard"
            background = circularRippleBackground(fbText)
            layoutParams = LayoutParams(dp(32), dp(32))
            isClickable = true
            isFocusable = true
            setOnClickListener { onDismiss() }
        }
        header.addView(backBtn)

        // Title & Active Badge Container
        val titleContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
        }

        val titleText = TextView(context).apply {
            text = "Font Studio"
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fbText)
            setSingleLine(true)
        }
        titleContainer.addView(titleText)

        updateActiveFontBadgeText()
        activeFontBadge.apply {
            textSize = 11f
            setTextColor(fbActiveStroke)
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        titleContainer.addView(activeFontBadge)

        header.addView(titleContainer)

        // Reset to Default button
        val resetBtn = TextView(context).apply {
            text = "Reset"
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(fbText)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = roundedRippleBackground(fbChipBg, fbCardStroke, 12)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                activateFont("default")
            }
        }
        header.addView(resetBtn)

        // Full Settings Studio Icon
        val settingsBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_key_settings)
            imageTintList = ColorStateList.valueOf(fbText)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(6), dp(6), dp(6), dp(6))
            contentDescription = "Font Studio Settings"
            background = circularRippleBackground(fbText)
            layoutParams = LayoutParams(dp(32), dp(32)).apply {
                marginStart = dp(4)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(context, SettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        }
        header.addView(settingsBtn)

        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(42)))
    }

    private fun updateActiveFontBadgeText() {
        val current = CustomFontManager.getFontItem(context, prefs.keyboardFont)
        val name = current?.name ?: "System Default"
        activeFontBadge.text = "Active: $name"
    }

    private fun buildCategoryChips() {
        categoryScroll.isHorizontalScrollBarEnabled = false
        categoryScroll.overScrollMode = OVER_SCROLL_NEVER

        categoryRow.orientation = HORIZONTAL
        categoryRow.gravity = Gravity.CENTER_VERTICAL
        categoryRow.setPadding(dp(8), dp(2), dp(8), dp(4))

        val categories = CustomFontManager.CATEGORIES
        categories.forEach { category ->
            val count = if (category == "All") allFonts.size else allFonts.count { it.category == category }
            if (count > 0 || category == "All") {
                val chip = TextView(context).apply {
                    text = category
                    textSize = 11.5f
                    typeface = Typeface.create(Typeface.DEFAULT, if (category == selectedCategory) Typeface.BOLD else Typeface.NORMAL)
                    setPadding(dp(10), dp(4), dp(10), dp(4))
                    gravity = Gravity.CENTER
                    updateChipStyle(this, category == selectedCategory)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        selectedCategory = category
                        filterFontsByCategory()
                        refreshCategoryChips()
                    }
                }
                categoryRow.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(6)
                })
            }
        }

        categoryScroll.addView(categoryRow)
        addView(categoryScroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun updateChipStyle(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setTextColor(Color.WHITE)
            chip.background = GradientDrawable().apply {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(fbActiveStroke)
            }
        } else {
            chip.setTextColor(fbMutedText)
            chip.background = GradientDrawable().apply {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(fbChipBg)
                setStroke(dp(1), fbCardStroke)
            }
        }
    }

    private fun refreshCategoryChips() {
        for (i in 0 until categoryRow.childCount) {
            val chip = categoryRow.getChildAt(i) as? TextView ?: continue
            val isSelected = chip.text.toString() == selectedCategory
            chip.typeface = Typeface.create(Typeface.DEFAULT, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            updateChipStyle(chip, isSelected)
        }
    }

    private fun filterFontsByCategory() {
        displayedFonts = if (selectedCategory == "All") {
            allFonts
        } else {
            allFonts.filter { it.category == selectedCategory }
        }
        fontAdapter.notifyDataSetChanged()
        fontGrid.scrollToPosition(0)
    }

    private fun buildFontGrid() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val spanCount = if (isLandscape) 4 else 3

        fontGrid.layoutManager = GridLayoutManager(context, spanCount)
        fontGrid.overScrollMode = OVER_SCROLL_NEVER
        fontGrid.clipToPadding = false
        fontGrid.setPadding(dp(6), dp(4), dp(6), dp(8))

        fontAdapter = FontGridAdapter()
        fontGrid.adapter = fontAdapter

        addView(fontGrid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun activateFont(fontId: String) {
        prefs.keyboardFont = fontId
        updateActiveFontBadgeText()
        fontAdapter.notifyDataSetChanged()
        onFontSelected(fontId)

        // Haptic feedback
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        val font = CustomFontManager.getFontItem(context, fontId)
        val name = font?.name ?: "System Default"
        Toast.makeText(context, "Active Font: $name", Toast.LENGTH_SHORT).show()
    }

    private fun circularRippleBackground(inkColor: Int): RippleDrawable {
        val bgShape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(inkColor, 18))
        }
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(inkColor, 75)),
            bgShape,
            mask
        )
    }

    private fun roundedRippleBackground(bgColor: Int, strokeColor: Int, radiusDp: Int): RippleDrawable {
        val bgShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(bgColor)
            setStroke(dp(1), strokeColor)
        }
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(colors.action, 75)),
            bgShape,
            mask
        )
    }

    private inner class FontGridAdapter : RecyclerView.Adapter<FontViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
            val card = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(7), dp(6), dp(7))
                isClickable = true
                isFocusable = true
            }

            val nameView = TextView(context).apply {
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

            val sampleView = TextView(context).apply {
                textSize = 13.5f
                gravity = Gravity.CENTER
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(3)
                }
            }

            val activeIndicator = TextView(context).apply {
                text = "✓ Active"
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(fbActiveStroke)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(2)
                }
            }

            card.addView(nameView)
            card.addView(sampleView)
            card.addView(activeIndicator)

            val lp = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            card.layoutParams = lp

            return FontViewHolder(card, nameView, sampleView, activeIndicator)
        }

        override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
            val item = displayedFonts[position]
            val isActive = item.id == prefs.keyboardFont

            holder.nameView.text = item.name
            holder.nameView.setTextColor(if (isActive) fbActiveStroke else fbText)

            // Styled preview sample text
            val sampleText = when {
                item.id == "default" -> "Default Abc"
                item.isCustomFile -> "Custom Aa"
                else -> item.transformer("Abc 123")
            }
            holder.sampleView.text = sampleText
            holder.sampleView.setTextColor(if (isActive) fbActiveStroke else fbText)

            // Apply custom typeface if applicable
            holder.sampleView.typeface = CustomFontManager.getTypeface(context, item.id)

            // Active indicator
            holder.activeIndicator.visibility = if (isActive) View.VISIBLE else View.GONE

            // Card background styling
            val cardBg = if (isActive) fbActiveBg else fbCardBg
            val stroke = if (isActive) fbActiveStroke else fbCardStroke
            val strokeWidth = if (isActive) dp(1.8f) else dp(1)

            val shapeDrawable = GradientDrawable().apply {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(cardBg)
                setStroke(strokeWidth, stroke)
            }
            val maskDrawable = GradientDrawable().apply {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.WHITE)
            }
            holder.card.background = RippleDrawable(
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(fbActiveStroke, 60)),
                shapeDrawable,
                maskDrawable
            )

            holder.card.setOnClickListener {
                activateFont(item.id)
            }
        }

        override fun getItemCount(): Int = displayedFonts.size
    }

    private class FontViewHolder(
        val card: LinearLayout,
        val nameView: TextView,
        val sampleView: TextView,
        val activeIndicator: TextView
    ) : RecyclerView.ViewHolder(card)
}
