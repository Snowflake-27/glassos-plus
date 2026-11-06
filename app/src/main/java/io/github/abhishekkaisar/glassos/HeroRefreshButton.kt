package io.github.abhishekkaisar.glassos.personal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.KeyEvent
import android.view.View

/** Native focus/click target drawn in the same capsule language as the Hero action. */
class HeroRefreshButton(ctx: Context) : View(ctx) {
    var navigate: ((Int) -> Boolean)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arc = RectF()
    private val arrow = Path()
    init {
        isFocusable = true
        isClickable = true
        contentDescription = "Shuffle featured title"
        setOnFocusChangeListener { _, _ -> invalidate() }
    }
    override fun onDraw(canvas: Canvas) {
        val size = width.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = if (isFocused) 0xFFF4F5F7.toInt() else 0x6640444C
        canvas.drawRoundRect(0f, 0f, size, height.toFloat(), size / 2, size / 2, paint)
        paint.color = if (isFocused) 0xFF111318.toInt() else 0xFFFFFFFF.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * .055f
        paint.strokeCap = Paint.Cap.ROUND
        arc.set(size * .27f, size * .27f, size * .73f, size * .73f)
        canvas.drawArc(arc, 210f, 135f, false, paint)
        canvas.drawArc(arc, 30f, 135f, false, paint)
        paint.style = Paint.Style.FILL
        for (rotation in floatArrayOf(0f, 180f)) {
            canvas.save(); canvas.rotate(rotation, size / 2, size / 2)
            arrow.reset(); arrow.moveTo(size * .76f, size * .30f)
            arrow.lineTo(size * .77f, size * .48f); arrow.lineTo(size * .60f, size * .42f); arrow.close()
            canvas.drawPath(arrow, paint); canvas.restore()
        }
    }
    override fun onKeyDown(code: Int, event: KeyEvent): Boolean {
        if (navigate?.invoke(code) == true) return true
        return super.onKeyDown(code, event)
    }
}
