package org.slashboard.ime.ime

import android.view.View
import android.view.inputmethod.EditorInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric

@RunWith(RobolectricTestRunner::class)
class ServiceDrawTest {
    @Test
    fun testServiceDraws() {
        val controller = Robolectric.buildService(SlashboardInputMethodService::class.java)
        controller.create()
        val service = controller.get()
        val view = service.onCreateInputView()
        
        val info = EditorInfo().apply { inputType = android.text.InputType.TYPE_CLASS_TEXT }
        service.onStartInput(info, false)
        service.onStartInputView(info, false)
        
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 1080, 800)
        
        // Try drawing to a canvas to trigger onDraw
        val bitmap = android.graphics.Bitmap.createBitmap(1080, 800, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)

        val downTime = android.os.SystemClock.uptimeMillis()
        val down = android.view.MotionEvent.obtain(downTime, downTime, android.view.MotionEvent.ACTION_DOWN, 200f, 300f, 0)
        view.dispatchTouchEvent(down)
        val move = android.view.MotionEvent.obtain(downTime, downTime + 20, android.view.MotionEvent.ACTION_MOVE, 200f, 360f, 0)
        view.dispatchTouchEvent(move)
        val up = android.view.MotionEvent.obtain(downTime, downTime + 50, android.view.MotionEvent.ACTION_UP, 200f, 360f, 0)
        view.dispatchTouchEvent(up)

        // Multiple consecutive touches across the keyboard to verify no state lock or freeze
        val keyPositions = listOf(100f to 250f, 300f to 250f, 500f to 350f, 700f to 450f)
        var time = downTime + 100
        for ((x, y) in keyPositions) {
            val d = android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
            view.dispatchTouchEvent(d)
            time += 40
            val u = android.view.MotionEvent.obtain(time - 40, time, android.view.MotionEvent.ACTION_UP, x, y, 0)
            view.dispatchTouchEvent(u)
            time += 40
        }
    }
}
