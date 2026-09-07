package org.slashboard.ime.ime

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import org.slashboard.ime.R
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * High-performance, zero-jitter in-place Voice Typing Panel for Slashboard IME.
 */
internal class VoiceTypingBoard(
    context: Context,
    private val colors: KeyboardColors,
    private val actions: KeyboardActions,
    private var useEnglish: Boolean,
    private val onDismiss: () -> Unit,
    private val onToggleLanguage: () -> Unit
) : LinearLayout(context) {

    private val dp = { value: Number ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }
    private val dpF = { value: Number ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
    }

    private var voiceManager: VoiceInputManager? = null
    private var isListening = false
    private var transcriptText: String = ""
    private var partialTranscript: String = ""

    // Views
    private lateinit var statusDot: View
    private lateinit var statusTextView: TextView
    private lateinit var langBadgeView: TextView
    private lateinit var transcriptScrollView: ScrollView
    private lateinit var transcriptTextView: TextView
    private lateinit var waveformView: WaveformVisualizerView
    private lateinit var micButton: FrameLayout
    private lateinit var micGlowRing: View
    private lateinit var micIconView: ImageView
    private lateinit var keyboardToggleBtn: ImageView
    private lateinit var backspaceBtn: ImageView

    private var cursorBlinkAnimator: ValueAnimator? = null
    private var micPulseAnimator: ValueAnimator? = null
    private var showCursor = true

    private val voiceListener = object : VoiceInputListener {
        override fun onReady() {
            setListeningState(true, "Listening... Speak now")
        }

        override fun onBeginningOfSpeech() {
            setListeningState(true, "Listening...")
        }

        override fun onRmsChanged(rmsdB: Float) {
            waveformView.updateRms(rmsdB)
        }

        override fun onPartialResult(text: String) {
            partialTranscript = text
            updateTranscriptDisplay()
        }

        override fun onVoiceResult(text: String) {
            partialTranscript = ""
            if (transcriptText.isEmpty()) {
                transcriptText = text
            } else {
                transcriptText += " " + text
            }
            updateTranscriptDisplay()
            setListeningState(false, "Done")
        }

        override fun onError(error: Int) {
            partialTranscript = ""
            val msg = if (error == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                "Didn't catch that. Tap mic to try again."
            } else if (error == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                "Speech timed out. Tap mic to speak."
            } else {
                "Tap mic to speak"
            }
            setListeningState(false, msg)
        }

        override fun onStateChanged(listening: Boolean, status: String) {
            setListeningState(listening, status)
        }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false
        clipToPadding = false
        
        // Deep obsidian aesthetic background
        background = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(
                Color.parseColor("#0F172A"),
                Color.parseColor("#090D16")
            )
        }

        buildUi()
        startCursorBlink()
    }

    private fun buildUi() {
        setPadding(dp(12), dp(8), dp(12), dp(8))

        // 1. TOP HEADER & STATUS ROW
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(28)).apply {
                bottomMargin = dp(6)
            }
        }

        // Status Indicator Pill (Glassmorphic)
        val statusPill = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(10), dp(3))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dpF(12)
                setStroke(dp(1), Color.parseColor("#334155"))
            }
        }

        statusDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                rightMargin = dp(6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#10B981"))
            }
        }
        statusPill.addView(statusDot)

        statusTextView = TextView(context).apply {
            text = "Listening..."
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        statusPill.addView(statusTextView)
        headerRow.addView(statusPill, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        // Language Pill
        langBadgeView = TextView(context).apply {
            text = if (useEnglish) "English (US)" else "සිංහල (LK)"
            setTextColor(Color.parseColor("#06B6D4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#132A3E"))
                cornerRadius = dpF(12)
                setStroke(dp(1), Color.parseColor("#06B6D4"))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onToggleLanguage()
            }
        }
        headerRow.addView(langBadgeView, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        addView(headerRow)

        // 2. LIVE TRANSCRIPT CARD (High contrast, scrollable text area)
        val transcriptCard = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.2f).apply {
                bottomMargin = dp(8)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161F30"))
                cornerRadius = dpF(12)
                setStroke(dp(1), Color.parseColor("#1E293B"))
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        transcriptScrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            isVerticalScrollBarEnabled = false
        }

        transcriptTextView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#F8FAFC"))
            typeface = Typeface.DEFAULT
            setLineSpacing(dpF(2), 1.1f)
            text = "Listening... Speak now |"
        }
        transcriptScrollView.addView(transcriptTextView)
        transcriptCard.addView(transcriptScrollView)

        addView(transcriptCard)

        // 3. REACTIVE AUDIO WAVEFORM (Canvas Animation)
        waveformView = WaveformVisualizerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply {
                bottomMargin = dp(8)
            }
        }
        addView(waveformView)

        // 4. BOTTOM CONTROL BAR (Keyboard toggle, Glowing mic, Backspace)
        val controlBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(60))
        }

        // Left: Keyboard toggle button (Dismiss voice mode)
        keyboardToggleBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(48)).apply {
                leftMargin = dp(4)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_keyboard)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = glassButtonDrawable()
            contentDescription = "Switch to Keyboard"
            setOnClickListener {
                actions.onPressFeedback()
                voiceManager?.stopListening()
                onDismiss()
            }
        }
        controlBar.addView(keyboardToggleBtn)

        // Spacer
        controlBar.addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))

        // Center: Glowing pulsating Microphone button
        micButton = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(62))
            clipChildren = false
            clipToPadding = false
        }

        micGlowRing = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(62), dp(62), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2210B981"))
            }
        }
        micButton.addView(micGlowRing)

        val micInnerButton = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                orientation = GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(
                    Color.parseColor("#06B6D4"),
                    Color.parseColor("#10B981")
                )
            }
        }

        micIconView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER)
            setImageResource(R.drawable.ic_key_mic)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            contentDescription = "Toggle Voice Input"
        }
        micInnerButton.addView(micIconView)
        micButton.addView(micInnerButton)

        micButton.setOnClickListener {
            actions.onPressFeedback()
            voiceManager?.toggleListening(useEnglish)
        }
        controlBar.addView(micButton)

        // Spacer
        controlBar.addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))

        // Right: Backspace Button
        backspaceBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(48)).apply {
                rightMargin = dp(4)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_key_backspace)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = glassButtonDrawable()
            contentDescription = "Backspace"
            setOnClickListener {
                actions.onPressFeedback()
                actions.onBackspace(false)
                if (partialTranscript.isNotEmpty()) {
                    partialTranscript = ""
                } else if (transcriptText.isNotEmpty()) {
                    val words = transcriptText.trim().split(" ")
                    transcriptText = if (words.size > 1) {
                        words.dropLast(1).joinToString(" ")
                    } else ""
                }
                updateTranscriptDisplay()
            }
            setOnLongClickListener {
                actions.onPressFeedback()
                actions.onBackspace(true)
                transcriptText = ""
                partialTranscript = ""
                updateTranscriptDisplay()
                true
            }
        }
        controlBar.addView(backspaceBtn)

        addView(controlBar)
    }

    private fun glassButtonDrawable(): RippleDrawable {
        val shape = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B"))
            cornerRadius = dpF(12)
            setStroke(dp(1), Color.parseColor("#334155"))
        }
        val mask = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dpF(12)
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#38BDF8")), shape, mask)
    }

    fun attachVoiceManager(manager: VoiceInputManager, isEnglish: Boolean) {
        this.voiceManager = manager
        this.useEnglish = isEnglish
        manager.addListener(voiceListener)
        langBadgeView.text = if (useEnglish) "English (US)" else "සිංහල (LK)"
        if (!manager.isListening) {
            manager.startListening(useEnglish)
        } else {
            setListeningState(true, "Listening...")
        }
    }

    fun updateLanguage(isEnglish: Boolean) {
        this.useEnglish = isEnglish
        langBadgeView.text = if (useEnglish) "English (US)" else "සිංහල (LK)"
        if (isListening) {
            voiceManager?.startListening(useEnglish)
        }
    }

    private fun setListeningState(listening: Boolean, status: String) {
        isListening = listening
        statusTextView.text = status
        waveformView.setActive(listening)

        if (listening) {
            statusDot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#10B981"))
            }
            startMicPulse()
        } else {
            statusDot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#64748B"))
            }
            stopMicPulse()
        }
        updateTranscriptDisplay()
    }

    private fun updateTranscriptDisplay() {
        val cursor = if (showCursor) "▌" else " "
        val displayText = when {
            partialTranscript.isNotEmpty() -> {
                if (transcriptText.isNotEmpty()) "$transcriptText $partialTranscript $cursor"
                else "$partialTranscript $cursor"
            }
            transcriptText.isNotEmpty() -> "$transcriptText $cursor"
            isListening -> "Listening... Speak now $cursor"
            else -> "Tap the microphone to speak"
        }
        transcriptTextView.text = displayText
        transcriptScrollView.post {
            transcriptScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun startCursorBlink() {
        cursorBlinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                showCursor = (it.animatedValue as Float) > 0.5f
                updateTranscriptDisplay()
            }
            start()
        }
    }

    private fun startMicPulse() {
        if (micPulseAnimator != null && micPulseAnimator!!.isRunning) return
        micPulseAnimator = ValueAnimator.ofFloat(1.0f, 1.45f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                micGlowRing.scaleX = scale
                micGlowRing.scaleY = scale
                micGlowRing.alpha = (1.5f - scale).coerceIn(0.1f, 0.7f)
            }
            start()
        }
    }

    private fun stopMicPulse() {
        micPulseAnimator?.cancel()
        micPulseAnimator = null
        micGlowRing.scaleX = 1f
        micGlowRing.scaleY = 1f
        micGlowRing.alpha = 0f
    }

    fun release() {
        voiceManager?.removeListener(voiceListener)
        cursorBlinkAnimator?.cancel()
        micPulseAnimator?.cancel()
        waveformView.stop()
    }

    /**
     * Canvas-based reactive audio visualizer inspired by high-performance canvas waveform animation.
     */
    internal class WaveformVisualizerView(context: Context) : View(context) {
        private val dpF = { value: Number ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
        }

        // 9 distinct vertical bars
        private val baseHeights = floatArrayOf(8f, 18f, 32f, 22f, 14f, 28f, 40f, 18f, 8f)
        private val currentHeights = FloatArray(9) { 8f }
        private var amplitudeFactor = 0f
        private var isActive = false
        private var isAttached = false

        private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private var shader: LinearGradient? = null
        private val barWidth = dpF(6)
        private val barGap = dpF(8)
        private val cornerRadius = dpF(4)
        private val maxHeight = dpF(44)
        private val minHeight = dpF(6)

        init {
            for (i in baseHeights.indices) {
                currentHeights[i] = dpF(baseHeights[i])
            }
        }

        fun setActive(active: Boolean) {
            isActive = active
            if (active) {
                postInvalidateOnAnimation()
            }
        }

        fun updateRms(rmsdB: Float) {
            // Normalize RMS (-2dB to 10dB+) to 0.0 .. 1.0
            val normalized = ((rmsdB + 2f) / 10f).coerceIn(0.1f, 1.4f)
            amplitudeFactor = normalized
            postInvalidateOnAnimation()
        }

        fun stop() {
            isActive = false
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            isAttached = true
            postInvalidateOnAnimation()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            isAttached = false
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (h > 0) {
                shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    Color.parseColor("#06B6D4"), // Electric Cyan
                    Color.parseColor("#10B981"), // Emerald Green
                    Shader.TileMode.CLAMP
                )
                barPaint.shader = shader
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val count = baseHeights.size
            val totalWidth = count * barWidth + (count - 1) * barGap
            val startX = (width - totalWidth) / 2f
            val centerY = height / 2f

            for (i in 0 until count) {
                val base = dpF(baseHeights[i])
                val targetHeight: Float
                if (isActive) {
                    val jitter = (Random.nextFloat() * 14f - 7f) * (amplitudeFactor.coerceAtLeast(0.3f))
                    val dynamicH = (base * (0.8f + amplitudeFactor * 0.8f) + dpF(jitter))
                        .coerceIn(minHeight, maxHeight)
                    // Smooth interpolation
                    currentHeights[i] = currentHeights[i] * 0.7f + dynamicH * 0.3f
                    targetHeight = currentHeights[i]
                } else {
                    currentHeights[i] = currentHeights[i] * 0.85f + minHeight * 0.15f
                    targetHeight = currentHeights[i]
                }

                val x = startX + i * (barWidth + barGap)
                val top = centerY - targetHeight / 2f
                val bottom = centerY + targetHeight / 2f

                canvas.drawRoundRect(x, top, x + barWidth, bottom, cornerRadius, cornerRadius, barPaint)
            }

            if (isActive && isAttached) {
                postInvalidateOnAnimation()
            }
        }
    }
}
