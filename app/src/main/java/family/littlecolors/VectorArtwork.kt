package family.littlecolors

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.json.JSONArray
import org.json.JSONObject

internal data class ColoredPath(val color: Int, val path: Path)

internal fun Picture.drawMarks(canvas: Canvas, pen: Paint) {
    pen.style = Paint.Style.FILL
    marks.forEach { pen.color = it.color; canvas.drawPath(it.path, pen) }
}

// The bundled artwork is normalized to absolute Bézier commands in board coordinates.
internal fun vectorPath(data: String): Path {
    val tokens = data.splitToSequence(' ').filter { it.isNotEmpty() }.iterator()
    fun number() = tokens.next().toFloat()
    val path = Path()
    while (tokens.hasNext()) {
        when (val command = tokens.next()) {
            "M" -> path.moveTo(number(),number())
            "L" -> path.lineTo(number(),number())
            "Q" -> path.quadTo(number(),number(),number(),number())
            "C" -> path.cubicTo(number(),number(),number(),number(),number(),number())
            "Z" -> path.close()
            else -> error("Unknown artwork command $command")
        }
    }
    return path
}

internal fun readPaths(items: JSONArray) = List(items.length()) { i ->
    val item = items.getJSONArray(i)
    ColoredPath(Color.parseColor(item.getString(0)), vectorPath(item.getString(1)))
}

internal fun Picture.drawIllustration(canvas: Canvas, pen: Paint) {
    regions.forEachIndexed { i, path ->
        pen.style = Paint.Style.FILL
        pen.color = previewColors[i]
        canvas.drawPath(path, pen)
    }
    drawMarks(canvas, pen)
    pen.style = Paint.Style.STROKE
    pen.color = ink
    pen.strokeWidth = 4.8f
    pen.strokeJoin = Paint.Join.ROUND
    outlines.forEach { canvas.drawPath(it, pen) }
}

private val art by lazy {
    val stream = requireNotNull(Picture::class.java.getResourceAsStream("/artwork/pages.json"))
    JSONObject(stream.bufferedReader().use { it.readText() })
}

internal fun vectorPage(name: String): Picture {
    val page = art.getJSONObject(name)
    val regions = readPaths(page.getJSONArray("regions"))
    val colors = page.getJSONArray("palette")
    val swatches = List(colors.length()) { i ->
        val swatch = colors.getJSONArray(i)
        swatch.getString(0) to Color.parseColor(swatch.getString(1))
    }
    return Picture(name,regions.map { it.path },swatches = swatches,previewColors = regions.map { it.color },
        marks = readPaths(page.getJSONArray("marks")))
}
