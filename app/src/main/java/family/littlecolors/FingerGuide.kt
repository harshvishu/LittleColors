package family.littlecolors

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View

/** A visual-only overlay. Input continues to the actual tile, canvas or button below. */
internal class FingerGuide(context: Context, private val continuous: () -> Boolean, val locate: (Float, PointF) -> Boolean) : View(context) {
    private val point = PointF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hand = Path().apply {
        moveTo(-4f,14f); lineTo(-4f,1f); cubicTo(-4f,-4f,3f,-5f,4f,0f)
        lineTo(6f,12f); quadTo(9f,7f,13f,12f); quadTo(17f,9f,20f,15f)
        quadTo(27f,13f,27f,20f); lineTo(26f,32f); quadTo(24f,43f,13f,42f)
        quadTo(7f,42f,2f,36f); lineTo(-10f,23f); cubicTo(-15f,17f,-10f,13f,-6f,17f)
        lineTo(-3f,20f); close()
    }
    private var starts = 0L
    private var requested = true
    private var held = false
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun step() { requested = true; if (!held) schedule(300) }
    fun touch(down: Boolean) {
        held = down
        if (down) { requested = false; invalidate() }
        else schedule(if (requested) 300 else 18_000)
    }
    private fun schedule(delay: Long) {
        starts = SystemClock.uptimeMillis() + delay
        invalidate()
    }
    override fun onDraw(c: Canvas) {
        val following = continuous()
        if (held && !following) return
        val now = SystemClock.uptimeMillis()
        val elapsed = now - starts
        if (!following && elapsed < 0) { postInvalidateDelayed(-elapsed); return }
        if (!following && elapsed >= 3600) { schedule(18_000); return }
        val t = if (following) {
            .5f + .5f * kotlin.math.sin((now % 1400) * (2.0 * Math.PI / 1400)).toFloat()
        } else (elapsed % 1200) / 1200f
        if (!locate(t,point)) { postInvalidateDelayed(80); return }
        c.save()
        c.translate(point.x,point.y)
        val d = resources.displayMetrics.density
        c.scale(d,d)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
        paint.color = 0x996e5599.toInt()
        c.drawCircle(0f,0f,8f+5*t,paint)
        paint.color = ink; paint.strokeWidth = 3.5f; paint.strokeJoin = Paint.Join.ROUND
        c.drawPath(hand,paint)
        paint.style = Paint.Style.FILL; paint.color = 0xffffd4ad.toInt()
        c.drawPath(hand,paint)
        c.restore()
        postInvalidateOnAnimation()
    }
}
