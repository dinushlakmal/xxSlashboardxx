package org.slashboard.ime.ime

import android.view.inputmethod.EditorInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class ServiceCrashTest {
    @Test
    fun testServiceStarts() {
        val controller = Robolectric.buildService(SlashboardInputMethodService::class.java)
        controller.create()
        val service = controller.get()
        val view = service.onCreateInputView()
        
        val info = EditorInfo().apply { inputType = android.text.InputType.TYPE_CLASS_TEXT }
        service.onStartInput(info, false)
        service.onStartInputView(info, false)
    }
}
