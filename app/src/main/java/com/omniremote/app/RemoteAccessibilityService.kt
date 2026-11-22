package com.omniremote.app
import android.accessibilityservice.AccessibilityService; import android.accessibilityservice.GestureDescription; import android.graphics.Path; import android.os.Bundle; import android.view.accessibility.AccessibilityEvent; import android.view.accessibility.AccessibilityNodeInfo
class RemoteAccessibilityService : AccessibilityService() {
    companion object { var instance: RemoteAccessibilityService? = null }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { super.onDestroy(); instance = null }
    fun tap(nx: Float, ny: Float) {
        val dm = resources.displayMetrics; val p = Path().apply { moveTo(nx.coerceIn(0f, 1f) * dm.widthPixels, ny.coerceIn(0f, 1f) * dm.heightPixels) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 50)).build(), null, null)
    }
    fun swipe(nx1: Float, ny1: Float, nx2: Float, ny2: Float, dur: Long) {
        val dm = resources.displayMetrics; val p = Path().apply { moveTo(nx1.coerceIn(0f, 1f) * dm.widthPixels, ny1.coerceIn(0f, 1f) * dm.heightPixels); lineTo(nx2.coerceIn(0f, 1f) * dm.widthPixels, ny2.coerceIn(0f, 1f) * dm.heightPixels) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, dur.coerceIn(50, 2000))).build(), null, null)
    }
    fun performNav(action: String) {
        when (action) { "back" -> performGlobalAction(GLOBAL_ACTION_BACK); "home" -> performGlobalAction(GLOBAL_ACTION_HOME); "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS); "power" -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
    }
    fun inputText(text: String) {
        val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}
