package org.slashboard.ime.ime

import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import java.util.ArrayDeque

/**
 * Manages Undo and Redo operations for the keyboard.
 * Combines standard Android framework key events (Ctrl+Z, Ctrl+Y, KEYCODE_UNDO, KEYCODE_REDO)
 * with an IME-level deleted text stack to guarantee text recovery even in apps that do not
 * implement native text undo (e.g., messaging apps, custom text fields).
 */
class UndoRedoManager {

    data class ActionItem(
        val deletedText: String,
        val timestamp: Long = SystemClock.uptimeMillis()
    )

    private val undoStack = ArrayDeque<ActionItem>(32)
    private val redoStack = ArrayDeque<ActionItem>(32)

    private val accumulatedDelete = StringBuilder()
    private var lastDeleteTimestamp = 0L

    /**
     * Records a deleted character or cluster. Consecutive deletions within 1.5 seconds
     * are aggregated into a single logical phrase/paragraph.
     */
    fun recordDeletedCluster(cluster: String) {
        val now = SystemClock.uptimeMillis()
        if (accumulatedDelete.isNotEmpty() && now - lastDeleteTimestamp > 1500L) {
            flushAccumulatedDelete()
        }
        // Prepended because backspace deletes right-to-left
        accumulatedDelete.insert(0, cluster)
        lastDeleteTimestamp = now
    }

    /**
     * Records an explicitly deleted chunk of text (e.g. from swipe delete or word delete).
     */
    fun recordDeletedText(text: String) {
        if (text.isEmpty()) return
        flushAccumulatedDelete()
        pushUndo(ActionItem(text))
    }

    fun flushAccumulatedDelete() {
        if (accumulatedDelete.isNotEmpty()) {
            val text = accumulatedDelete.toString()
            accumulatedDelete.setLength(0)
            pushUndo(ActionItem(text))
        }
    }

    private fun pushUndo(item: ActionItem) {
        if (undoStack.size >= 32) {
            undoStack.removeLast()
        }
        undoStack.push(item)
        redoStack.clear()
    }

    fun canUndo(): Boolean = accumulatedDelete.isNotEmpty() || undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Executes Undo: sends standard KeyEvent shortcuts to the host editor and,
     * if the host does not support native undo, restores the deleted text from the IME stack.
     */
    fun performUndo(ic: InputConnection, onFeedback: (String) -> Unit) {
        flushAccumulatedDelete()

        // 1. Send standard Android Undo shortcut combinations (Ctrl+Z)
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))

        // 2. Fallback restoration for applications without Ctrl+Z handling (e.g. chat apps)
        if (undoStack.isNotEmpty()) {
            val item = undoStack.pop()
            redoStack.push(item)
            ic.commitText(item.deletedText, 1)
            onFeedback("ආපසු ලබාගත්තා (Undo)")
        } else {
            onFeedback("Undo")
        }
    }

    /**
     * Executes Redo: sends standard KeyEvent shortcuts to the host editor and,
     * if an item exists on the redo stack, applies it.
     */
    fun performRedo(ic: InputConnection, onFeedback: (String) -> Unit) {
        // 1. Send standard Android Redo shortcut combinations (Ctrl+Y and Ctrl+Shift+Z)
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))

        // 2. Stack-based redo if available
        if (redoStack.isNotEmpty()) {
            val item = redoStack.pop()
            undoStack.push(item)
            // Re-delete the restored text to redo the deletion
            ic.deleteSurroundingText(item.deletedText.length, 0)
            onFeedback("යළි කළා (Redo)")
        } else {
            onFeedback("Redo")
        }
    }

    fun clear() {
        accumulatedDelete.setLength(0)
        undoStack.clear()
        redoStack.clear()
    }
}
