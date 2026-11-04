package io.github.abhishekkaisar.glassos.personal

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/** Window-change events only: no window contents, key filtering, gestures or volume handling. */
class FastHomeService : AccessibilityService() {
    private var homes: Set<String> = emptySet()
    private var lastStart = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        WLog.init(this)
        homes = Platform.otherHomeActivities(this)
        connected = true
        // Stop usage polling while this event-driven helper is bound; restart it if the helper disconnects.
        HomeWatcher.sync(this)
        WLog.i("fast Home connected; stock activities ${homes.size}; no key filtering")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        val cls = event.className?.toString()
        // An explicit visit to stock Home ends when another activity (or Personal itself) opens.
        if (pkg == packageName || (pkg != null && !Platform.isStockHomePkg(this, pkg) && cls?.endsWith("Activity") == true)) {
            HomeWatcher.userChoseAmazon = false
        }
        val allowed = !HomeWatcher.userChoseAmazon && SystemClock.elapsedRealtime() >= HomeWatcher.allowAmazonUntil
        if (!HomeRedirectPolicy.accepts(Prefs(this).replaceHome, allowed,
                pkg, cls, homes)) return
        val now = SystemClock.uptimeMillis()
        if (lastStart != 0L && now - lastStart < 250L) return
        lastStart = now
        try {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
            WLog.i("fast Home redirect; event delivery ${now - event.eventTime} ms")
        } catch (e: Exception) {
            WLog.i("fast Home start refused: ${e.javaClass.simpleName}")
        }
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        connected = false
        HomeWatcher.sync(this)
        super.onDestroy()
    }

    companion object {
        @Volatile var connected = false
            private set
    }
}
