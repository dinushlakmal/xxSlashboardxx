    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefs = KeyboardPreferences(this)
        restricted = info?.let(::isRestrictedEditor) ?: true
        editorLayout = editorLayout(info)
        keyboard.configure(prefs.mode, offerSystemSwitch(), enterLabel(info), editorLayout)
        keyboard.learningEnabled = !restricted && editorLayout == EditorLayout.TEXT
        checkOtp()
        if (prefs.clipboardHistory) captureClipboard()
        clipboardHistory?.let { keyboard.setClipboardItems(it.items(), it.pinnedItems()) }
        listenForClipboard()
        keyboard.setRecentEmoji(recentEmoji)
        updateSuggestions()
    }

    override fun onFinishInput() {
        deleteAnchor = -1
        deleteLength = 0
        cancelComposition(false)
        super.onFinishInput()
    }

    override fun onDestroy() {
        voiceInputManager?.destroy()
        soundPool?.release()
        stopClipboardListener()
        serviceScope.cancel()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopClipboardListener()
        cancelComposition(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composition.active && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            cancelComposition(false)
        }
        lastSelectionEnd = newSelEnd
    }
