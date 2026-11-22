package family.littlecolors

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

internal class Confetti(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var start = 0L
    private var finale = false
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun burst(big: Boolean) {
        finale = big
        start = SystemClock.uptimeMillis()
        invalidate()
    }
    override fun onDraw(c: Canvas) {
        val t = (SystemClock.uptimeMillis()-start) / (if (finale) 2400f else 720f)
        if (t >= 1f || start == 0L) return
        val d = resources.displayMetrics.density
        val count = if (finale) 84 else 16
        for (i in 0 until count) {
            val seed = (i*71%97)/97f
            val side = if (i%2==0) 1f else -1f
            val time = (t - (if (finale) .28f else .08f)*seed).coerceAtLeast(0f)
            val reach = if (finale) .82f else .26f
            val x = (if (side > 0) 0f else width.toFloat()) + side*width*(.04f+reach*(.24f+seed)*time)
            val y = height*((if (finale) .74f else .28f) - (1.8f+seed)*time + 2.65f*time*time)
            paint.color = palette[i%palette.size].second
            paint.alpha = (255*((1-t)*4).coerceIn(0f,1f)).toInt()
            c.save(); c.translate(x,y); c.rotate(i*19f+time*240f)
            val w = d*(2f+2f*kotlin.math.abs(sin(i+time*12f)))
            c.drawRoundRect(-w,-5*d,w,5*d,d,d,paint); c.restore()
        }
        postInvalidateOnAnimation()
    }
}
