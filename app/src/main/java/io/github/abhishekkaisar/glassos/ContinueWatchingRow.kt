package io.github.abhishekkaisar.glassos.personal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import kotlin.math.ceil

/** Reusable, virtual horizontal carousel: only visible cards are drawn, with a seamless modular loop. */
class ContinueWatchingRow(ctx: Context) : View(ctx) {
    var onUp: (() -> Unit)? = null
    var onDown: (() -> Unit)? = null
    var onOpen: ((ContinueItem) -> Unit)? = null
    var onEnableOrRetry: (() -> Unit)? = null
    var canAutoAdvance: () -> Boolean = { true }
    private var state = ContinueState(emptyList(), "Loading TV watch history…", false)
    private var index = 0
    private var phase = 0f
    private var animation: ValueAnimator? = null
    private var active = false
    private var autoScroll = false
    private var automaticAnimation = false
    private var lastInteraction = 0L
    private val main = Handler(Looper.getMainLooper())
    private val art = ContinueArtwork(ctx) { invalidate() }
    private val glass = GlassPainter().apply { tint = .04f; dark = .35f; border = .18f; grain = .15f }
    private val rect = RectF()
    private val imageRect = RectF()
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Fonts.medium }
    private val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Fonts.medium }
    private val detail = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCED8.toInt(); typeface = Fonts.regular }
    private val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ -> noteInteraction() }
    private val tick = object : Runnable {
        override fun run() {
            if (!active || !isAttachedToWindow) return
            if (autoScroll && ContinueLogic.mayAdvance(canAutoAdvance(), hasFocus(), state.items.size, SystemClock.uptimeMillis() - lastInteraction)) move(1, true)
            schedule(5000)
        }
    }
    init {
        id = generateViewId(); isFocusable = true; isClickable = true
        setOnClickListener { open() }
    }
    private val unit get() = resources.displayMetrics.widthPixels / 1920f
    private fun px(v: Float) = v * unit
    private var cardWidth = 1
    private var cardGap = 0
    private var cardInset = 0
    private var radius = 0f
    fun configure(width: Int, gap: Int, inset: Int, cornerRadius: Float) {
        animation?.end()
        cardWidth = width; cardGap = gap; cardInset = inset; radius = cornerRadius
        invalidate()
    }
    fun setAutoScrollEnabled(value: Boolean) {
        if (autoScroll == value) return
        autoScroll = value
        if (!value && automaticAnimation) animation?.end()
        noteInteraction()
    }
    fun refreshAutoScroll() { schedule(9000) }
    private val imageHeight get() = cardWidth * 9 / 16
    private val step get() = cardWidth + cardGap.toFloat()
    private val inset get() = cardInset.toFloat()
    fun setState(value: ContinueState) {
        if (state.items != value.items) {
            animation?.end()
            val keep = state.items.getOrNull(index)?.identity
            state = value
            index = state.items.indexOfFirst { it.identity == keep }.coerceAtLeast(0)
            phase = 0f
        } else state = value
        updateDescription(); invalidate(); schedule(5000)
    }
    fun setActive(value: Boolean) { active = value; if (!value) animation?.end(); schedule(5000) }
    fun noteInteraction() {
        if (automaticAnimation) animation?.end()
        lastInteraction = SystemClock.uptimeMillis(); schedule(9000)
    }
    private fun schedule(delay: Long) {
        main.removeCallbacks(tick)
        if (autoScroll && active && isAttachedToWindow && visibility == VISIBLE && !hasFocus() && state.items.size > 1) main.postDelayed(tick, delay)
    }
    private fun move(delta: Int, automatic: Boolean = false) {
        if (state.items.size <= 1) return
        animation?.end()
        automaticAnimation = automatic
        val next = ContinueLogic.wrap(index + delta, state.items.size)
        animation = ValueAnimator.ofFloat(0f, delta.toFloat()).apply {
            duration = 420; interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) { automaticAnimation = false; index = next; phase = 0f; updateDescription(); invalidate() }
            })
            start()
        }
    }
    private fun open() {
        noteInteraction(); animation?.end()
        state.items.getOrNull(index)?.let { onOpen?.invoke(it) } ?: onEnableOrRetry?.invoke()
    }
    private fun updateDescription() {
        contentDescription = state.items.getOrNull(index)?.let { "Continue Watching: ${it.title}, ${it.app}" } ?: "Continue Watching. ${state.message}"
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); viewTreeObserver.addOnGlobalFocusChangeListener(focusListener); schedule(5000) }
    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnGlobalFocusChangeListener(focusListener)
        main.removeCallbacks(tick); animation?.end(); super.onDetachedFromWindow()
    }
    override fun onFocusChanged(gain: Boolean, direction: Int, previous: Rect?) {
        super.onFocusChanged(gain, direction, previous)
        if (gain) animation?.end()
        noteInteraction(); invalidate()
    }
    override fun onKeyDown(code: Int, e: KeyEvent): Boolean {
        noteInteraction()
        return when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { move(-1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { move(1); true }
            KeyEvent.KEYCODE_DPAD_UP -> { onUp?.invoke(); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { onDown?.invoke(); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { if (e.repeatCount == 0) performClick(); true }
            else -> super.onKeyDown(code, e)
        }
    }
    override fun onKeyUp(code: Int, e: KeyEvent): Boolean =
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) true else super.onKeyUp(code, e)
    override fun onTouchEvent(e: MotionEvent): Boolean {
        noteInteraction()
        if (e.action == MotionEvent.ACTION_UP) {
            requestFocus(); animation?.end()
            if (state.items.isNotEmpty()) index = ContinueLogic.wrap(index + ((e.x - inset) / step).toInt().coerceAtLeast(0), state.items.size)
            performClick()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        heading.textSize = px(24f); title.textSize = px(18f); detail.textSize = px(14f)
        c.drawText("Continue Watching", inset, px(25f), heading)
        if (state.items.isEmpty()) {
            rect.set(inset, px(40f), width - inset, height - px(16f))
            glass.draw(c, this, rect, px(24f))
            if (hasFocus()) { rim.color = Color.WHITE; rim.strokeWidth = px(3f); c.drawRoundRect(rect, px(24f), px(24f), rim) }
            val message = TextUtils.ellipsize(state.message, title, rect.width() - px(56f), TextUtils.TruncateAt.END).toString()
            c.drawText(message, rect.left + px(28f), rect.centerY(), title)
            return
        }
        val count = ceil(width / step).toInt() + 1
        val top = px(40f)
        for (slot in -1..minOf(count, state.items.size)) {
            if (phase == 0f && slot >= state.items.size) continue
            val item = state.items[ContinueLogic.wrap(index + slot, state.items.size)]
            val x = inset + (slot - phase) * step
            if (x + cardWidth < 0 || x > width) continue
            rect.set(x, top, x + cardWidth, top + imageHeight + px(44f))
            c.save()
            path.reset(); path.addRoundRect(rect, radius, radius, Path.Direction.CW); c.clipPath(path)
            glass.draw(c, this, rect, radius)
            imageRect.set(x, top, x + cardWidth, top + imageHeight)
            val bitmap = art.get(item.art, cardWidth, imageHeight)
            if (bitmap != null) c.drawBitmap(bitmap, null, imageRect, paint)
            else { paint.color = 0xFF202B43.toInt(); c.drawRect(imageRect, paint); c.drawText(item.app, x + px(20f), imageRect.centerY(), title) }
            if (item.fraction >= 0f) {
                paint.color = 0x80505668.toInt(); c.drawRect(x, imageRect.bottom - px(3f), x + cardWidth, imageRect.bottom, paint)
                paint.color = 0xFF8CE8EA.toInt(); c.drawRect(x, imageRect.bottom - px(3f), x + cardWidth * item.fraction, imageRect.bottom, paint)
            }
            val name = TextUtils.ellipsize(item.title, title, cardWidth - px(30f), TextUtils.TruncateAt.END).toString()
            c.drawText(name, x + px(15f), imageRect.bottom + px(22f), title)
            val meta = listOfNotNull(item.app, item.season.takeIf { it.isNotBlank() }?.let { "S$it" },
                item.episode.takeIf { it.isNotBlank() }?.let { "E$it" }, item.episodeTitle.takeIf { it.isNotBlank() }).joinToString(" · ")
            c.drawText(TextUtils.ellipsize(meta, detail, cardWidth - px(30f), TextUtils.TruncateAt.END).toString(), x + px(15f), imageRect.bottom + px(40f), detail)
            c.restore()
        }
        if (hasFocus()) {
            rect.set(inset, top, inset + cardWidth, top + imageHeight + px(44f))
            rim.color = Color.WHITE; rim.strokeWidth = px(3f); c.drawRoundRect(rect, radius, radius, rim)
        }
    }
    fun release() { active = false; main.removeCallbacksAndMessages(null); animation?.end(); art.release() }
}
