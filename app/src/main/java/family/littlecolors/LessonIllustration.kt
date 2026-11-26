package family.littlecolors

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

internal class LessonIllustration(
    context: Context,
    private val lesson: LearningLesson,
    private val onItemCounted: ((Int) -> Unit)? = null,
) : View(context) {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val assigned = IntArray(lesson.count)
    private val bounceAt = LongArray(lesson.count)
    private val slots = Array(lesson.count) { RectF() }
    private var counted = 0
    private var downSlot = -1

    init {
        isSoundEffectsEnabled = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val perRow = min(lesson.count, if (width >= height) 5 else 3)
        val rows = ceil(lesson.count.toFloat() / perRow).toInt()
        val edge = min(width / (perRow + .25f), height / (rows + .20f))
        val scale = edge / 320f
        val now = SystemClock.uptimeMillis()
        var animating = false
        for (index in 0 until lesson.count) {
            val row = index / perRow
            val columns = min(perRow, lesson.count - row * perRow)
            val x = (width - columns * edge) / 2f + (index % perRow) * edge
            val y = (height - rows * edge) / 2f + row * edge + (edge - 300f * scale) / 2f
            slots[index].set(x, y, x + 320f * scale, y + 300f * scale)
            val elapsed = now - bounceAt[index]
            val pop = if (bounceAt[index] > 0L && elapsed < 260L) {
                animating = true
                1f + .16f * sin(Math.PI * elapsed / 260.0).toFloat()
            } else {
                1f
            }
            val cx = slots[index].centerX()
            val cy = slots[index].centerY()
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(pop, pop)
            canvas.translate(-cx, -cy)
            canvas.translate(x, y)
            canvas.scale(scale, scale)
            lesson.illustration.drawIllustration(canvas, pen)
            canvas.restore()
        }
        if (animating) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val callback = onItemCounted ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downSlot = slotAt(event.x, event.y)
                return downSlot >= 0
            }
            MotionEvent.ACTION_UP -> {
                val slot = downSlot
                downSlot = -1
                if (slot >= 0 && slot == slotAt(event.x, event.y)) {
                    val number = if (assigned[slot] > 0) {
                        if (counted >= lesson.count) {
                            assigned.fill(0)
                            counted = 1
                            assigned[slot] = 1
                            1
                        } else {
                            assigned[slot]
                        }
                    } else {
                        counted += 1
                        assigned[slot] = counted
                        counted
                    }
                    bounceAt[slot] = SystemClock.uptimeMillis()
                    invalidate()
                    callback(number)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                downSlot = -1
                return true
            }
        }
        return false
    }

    private fun slotAt(x: Float, y: Float): Int {
        for (index in slots.indices) {
            if (slots[index].contains(x, y)) return index
        }
        return -1
    }
}
