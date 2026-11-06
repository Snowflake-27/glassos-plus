package io.github.abhishekkaisar.glassos.personal

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Window-change redirect, plus opt-in HOME-only filtering. No window contents or other-key consumption. */
class FastHomeService : AccessibilityService() {
    private var homes: Set<String> = emptySet()
    private var lastStart = 0L
    private var handlingHome = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        WLog.init(this)
        homes = Platform.otherHomeActivities(this)
        connected = true
        instance = this
        configureKeyFilter()
        // Stop usage polling while this event-driven helper is bound; restart it if the helper disconnects.
        HomeWatcher.sync(this)
        WLog.i("fast Home connected; stock activities ${homes.size}; direct key ${Prefs(this).directHomeKey}")
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

    private fun configureKeyFilter() {
        val info = serviceInfo ?: return
        val prefs = Prefs(this)
        val enabled = prefs.replaceHome && prefs.directHomeKey
        info.flags = DirectHomeKeyPolicy.flags(info.flags, enabled)
        serviceInfo = info
        WLog.i("direct Home key filter: $enabled")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Consume UP only if its DOWN was accepted; never leak a half key sequence to the system.
        if (event.keyCode == KeyEvent.KEYCODE_HOME && event.action == KeyEvent.ACTION_UP && handlingHome) {
            handlingHome = false
            return true
        }
        val prefs = Prefs(this)
        if (!DirectHomeKeyPolicy.accepts(prefs.replaceHome && prefs.directHomeKey, event.keyCode)) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return handlingHome
        try {
            HomeWatcher.userChoseAmazon = false
            HomeWatcher.allowAmazonUntil = 0L
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
            handlingHome = true
            WLog.i("direct Home physical key accepted")
            return true
        } catch (e: Exception) {
            WLog.i("direct Home key start refused: ${e.javaClass.simpleName}")
            return false
        }
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        connected = false
        instance = null
        HomeWatcher.sync(this)
        super.onDestroy()
    }

    companion object {
        private var instance: FastHomeService? = null
        fun syncKeyFilter() { instance?.configureKeyFilter() }
        @Volatile var connected = false
            private set
    }
}
