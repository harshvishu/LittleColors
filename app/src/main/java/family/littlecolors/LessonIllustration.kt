package family.littlecolors

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.ceil
import kotlin.math.min

internal class LessonIllustration(context: Context, private val lesson: LearningLesson) : View(context) {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val perRow = min(lesson.count, if (width >= height) 5 else 3)
        val rows = ceil(lesson.count.toFloat() / perRow).toInt()
        val edge = min(width / (perRow + .25f), height / (rows + .20f))
        val scale = edge / 320f
        for (index in 0 until lesson.count) {
            val row = index / perRow
            val columns = min(perRow, lesson.count - row * perRow)
            val x = (width - columns * edge) / 2f + (index % perRow) * edge
            val y = (height - rows * edge) / 2f + row * edge + (edge - 300f * scale) / 2f
            canvas.save()
            canvas.translate(x, y)
            canvas.scale(scale, scale)
            lesson.illustration.drawIllustration(canvas, pen)
            canvas.restore()
        }
    }
}
