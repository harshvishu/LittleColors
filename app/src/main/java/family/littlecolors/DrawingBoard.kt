package family.littlecolors

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

internal val ink = Color.rgb(48, 41, 64)
internal val palette = listOf("Coral" to 0xfffa7481.toInt(), "Gold" to 0xffffbf46.toInt(),
    "Leaf" to 0xff58ca94.toInt(), "Sky" to 0xff58bcf2.toInt(), "Plum" to 0xffaf86ee.toInt(), "Peach" to 0xffff995e.toInt())

internal class DrawingBoard(context: Context, private val sound: (Cue) -> Unit, private val pencilSound: (Boolean) -> Unit, val changed: () -> Unit) : View(context) {
    var page = 0
        private set
    enum class Mode { ASSISTED, FOLLOW, FREEHAND }
    var mode = Mode.ASSISTED
        private set
    val freehand get() = mode == Mode.FREEHAND
    var erasing = false
        private set
    fun erase() { erasing = true; changed() }
    private val clearInk = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private var lastX = 0f
    private var lastY = 0f
    private val cursor = PointF()
    private val outlineMillis get() = maxOf(9000f, totalLength * 12f)
    private val fillMillis = 7000f
    private val picture get() = pictures[page]
    private var outline = 0f
    private var fill = 0f
    private val colors = mutableListOf<Int>()
    var selected: Int? = null
        private set
    val choosing get() = !freehand && outline == 1f && !done && selected == null
    val done get() = !freehand && colors.size == picture.regions.size
    val hint get() = when {
        freehand -> if (erasing) "Erase with your finger" else "Draw with your finger"
        mode == Mode.FOLLOW && !done && !choosing -> if (outline < 1f) "Follow the outline" else "Follow the pencil"
        done -> "Picture complete!"
        outline < 1 -> "Touch and hold to draw"
        choosing -> "Pick a color"
        else -> "Touch and hold to paint"
    }
    private data class Stroke(val color: Int, val erase: Boolean, val path: Path, val x: Float, val y: Float, var moved: Boolean = false)
    private val strokes = mutableListOf<Stroke>()
    private var pointer = -1
    private var lastTime = -1L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val measure = PathMeasure()
    private val segment = Path()
    private val bounds = RectF()
    private var lengths = picture.outlines.map { PathMeasure(it, false).length }
    private var totalLength = lengths.sum()
    private val scale get() = min(width / 300f, height / 288f).coerceAtLeast(.001f)
    private val left get() = (width - 320 * scale) / 2
    private val top get() = (height - 300 * scale) / 2

    private val dotted = DashPathEffect(floatArrayOf(.4f, 6.2f), 0f)
    private val tip = FloatArray(2)
    private val wood = Path().apply { moveTo(0f,0f); lineTo(9f,-4.8f); lineTo(9f,4.8f); close() }
    private val nib = Path().apply { moveTo(0f,0f); lineTo(3.6f,-2f); lineTo(3.6f,2f); close() }
    private data class Lane(val left: Float, val right: Float)
    private val tilt = .28f
    private val slant = Matrix().apply { setSkew(0f,-tilt) }
    private var paintPaths = emptyList<Path>()
    private val sparkle = Path().apply {
        moveTo(0f,-9f); quadTo(1f,-1f,7f,0f); quadTo(1f,1f,0f,9f)
        quadTo(-1f,1f,-7f,0f); quadTo(-1f,-1f,0f,-9f); close()
    }
    private var lanes = emptyList<List<Lane>>()

    private fun paintLanes(path: Path): List<Lane> {
        path.computeBounds(bounds, true)
        val region = Region().apply { setPath(path, Region(0,-100,320,310)) }
        val count = kotlin.math.ceil(bounds.height() / 18f).toInt().coerceAtLeast(1)
        return List(count) { row ->
            val y = (bounds.top + (row + .5f) * bounds.height() / count).toInt()
            val xs = bounds.left.toInt()..bounds.right.toInt()
            Lane((xs.firstOrNull { region.contains(it,y) } ?: xs.first).toFloat() - 5f,
                (xs.lastOrNull { region.contains(it,y) } ?: xs.last).toFloat() + 5f)
        }
    }

    fun open(index: Int, newMode: Mode) {
        cancelTouch()
        page = index
        mode = newMode
        erasing = false
        outline = 0f
        fill = 0f
        colors.clear()
        strokes.clear()
        selected = if (freehand) palette.first().second else null
        paintPaths = picture.regions.map { Path(it).apply { transform(slant) } }
        lanes = paintPaths.map(::paintLanes)
        lengths = picture.outlines.map { PathMeasure(it, false).length }
        totalLength = lengths.sum()
        invalidate()
        changed()
    }

    fun pick(color: Int) {
        if (freehand || (outline == 1f && !done)) {
            sound(Cue.SWATCH)
            erasing = false
            selected = color
            invalidate()
            changed()
        }
    }

    fun cancelTouch() {
        removeCallbacks(hushPencil)
        pencilSound(false)
        pointer = -1
        lastTime = -1
    }

    private fun advance(ms: Long) {
        advanceFraction(if (outline < 1f) outline + ms/outlineMillis else fill + ms/fillMillis)
    }

    private fun advanceFraction(value: Float) {
        if (outline < 1f) {
            outline = min(1f,value)
            if (outline == 1f) { cancelTouch(); sound(Cue.READY); changed() }
        } else if (selected != null && !done) {
            fill = min(1f,value)
            if (fill == 1f) {
                colors.add(selected!!)
                fill = 0f
                selected = null
                cancelTouch()
                sound(if (done) Cue.FINISH else Cue.READY)
                changed()
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (freehand || (!choosing && !done)) sound(Cue.TOUCH)
                if (freehand) {
                    pointer = e.getPointerId(0)
                    val x = (e.x - left) / scale
                    val y = (e.y - top) / scale
                    strokes.add(Stroke(selected!!, erasing, Path().apply { moveTo(x, y) }, x, y))
                } else if (mode == Mode.FOLLOW && !choosing && !done) {
                    pointer = e.getPointerId(0)
                    lastX = e.x; lastY = e.y
                } else if (!choosing && !done) {
                    lastTime = SystemClock.uptimeMillis()
                    if (outline < 1f) pencilSound(true)
                    advance(45)
                }
            }
            MotionEvent.ACTION_MOVE -> if (freehand) {
                val i = e.findPointerIndex(pointer)
                if (i >= 0) {
                    val stroke = strokes.last()
                    for (h in 0 until e.historySize) stroke.path.lineTo((e.getHistoricalX(i,h)-left)/scale, (e.getHistoricalY(i,h)-top)/scale)
                    stroke.path.lineTo((e.getX(i)-left)/scale, (e.getY(i)-top)/scale)
                    stroke.moved = true
                }
            }
            else if (mode == Mode.FOLLOW && pointer >= 0) {
                val i = e.findPointerIndex(pointer)
                if (i >= 0) {
                    for (h in 0 until e.historySize) follow(e.getHistoricalX(i,h),e.getHistoricalY(i,h))
                    follow(e.getX(i),e.getY(i))
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (pointer == e.getPointerId(e.actionIndex)) cancelTouch()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelTouch()
        }
        invalidate()
        return true
    }

    private val hushPencil = Runnable { pencilSound(false) }

    private fun follow(x: Float, y: Float) {
        if (pointer < 0 || done || choosing) return
        val distance = kotlin.math.hypot(x-lastX,y-lastY)/scale
        if (distance < .20f) return
        lastX = x; lastY = y
        val tracing = outline < 1f
        val from = if (tracing) outline else fill
        fun distanceToFinger(p: Float): Float {
            pointAt(p,tracing,cursor)
            return kotlin.math.hypot(cursor.x-x,cursor.y-y)/scale
        }
        var best = distanceToFinger(from)
        if (best > 28f) return
        var next = from
        val previous = PointF(cursor.x,cursor.y)
        var walked = 0f
        for (i in 1..80) {
            val candidate = minOf(1f, from+i*.001f)
            val error = distanceToFinger(candidate)
            val segment = kotlin.math.hypot(cursor.x-previous.x,cursor.y-previous.y)/scale
            // Move the prompt to the next disconnected contour once its predecessor ends.
            if (tracing && segment > 20f && best < 9f) { next = candidate; break }
            walked += segment
            if (walked > minOf(distance*1.8f+1f,32f)) break
            if (error < best || (candidate == 1f && error < 8f)) { next = candidate; best = error }
            previous.set(cursor.x,cursor.y)
            if (candidate == 1f) break
        }
        if (next <= from) return
        if (tracing) {
            pencilSound(true)
            removeCallbacks(hushPencil); postDelayed(hushPencil,160)
        }
        advanceFraction(next)
    }

    private fun line(color: Int, weight: Float) {
        paint.style = Paint.Style.STROKE; paint.color = color; paint.strokeWidth = weight
    }
    private fun solid(color: Int) { paint.style = Paint.Style.FILL; paint.color = color }

    private fun drawCrayon(c: Canvas, color: Int) {
        c.save()
        c.translate(tip[0], tip[1])
        c.rotate(-48f)
        solid(ink); c.drawRoundRect(7f,-5.8f,43f,5.8f,3f,3f,paint)
        solid(color); c.drawRoundRect(8f,-4.8f,42f,4.8f,2f,2f,paint)
        solid(0xffffdea9.toInt()); c.drawPath(wood,paint)
        solid(color); c.drawPath(nib,paint)
        solid(Color.WHITE); c.drawRect(13f,-4.7f,15f,4.7f,paint)
        when ((page + colors.size) % 3) {
            0 -> { // Candy stripes
                for (x in 19..36 step 7) c.drawLine(x.toFloat(),4f,x+4f,-4f,paint.apply { strokeWidth=2f })
            }
            1 -> { // Star wand
                for (x in 21..35 step 14) {
                    c.save(); c.translate(x.toFloat(),0f); c.scale(.37f,.43f)
                    c.drawPath(sparkle,paint); c.restore()
                }
                solid(0xffffce55.toInt()); c.drawCircle(42f,0f,5f,paint)
            }
            else -> { // Caterpillar pencil
                for (i in 0..3) {
                    solid(palette[(i+2)%palette.size].second)
                    c.drawCircle(19f+i*7f,0f,5.5f,paint)
                }
                solid(ink); c.drawCircle(40f,-2f,1f,paint)
            }
        }
        c.restore()
    }

    // Each diagonal stroke runs to the edge, then turns into the next stroke.
    private fun zigzag(index: Int, progress: Float, position: FloatArray) {
        val rows = lanes[index]
        val row = progress.toInt().coerceAtMost(rows.lastIndex)
        val lane = rows[row]
        val forward = row % 2 == 0
        val local = progress-row
        val sweep = if (row == rows.lastIndex) 1f else .84f
        val across = (local/sweep).coerceAtMost(1f)
        val fraction = if (forward) across else 1f-across
        val step = bounds.height()/rows.size
        var x = lane.left + (lane.right-lane.left)*fraction
        var y = bounds.top + (row+.5f)*step
        if (local > sweep && row < rows.lastIndex) {
            val turn = (local-sweep)/(1f-sweep)
            val next = rows[row+1]
            x += ((if (forward) next.right else next.left)-x)*turn
            y += step*turn
        }
        position[0] = x
        position[1] = y
    }

    private fun paintRegion(c: Canvas, path: Path, index: Int) {
        path.computeBounds(bounds, true)
        val rows = lanes[index]
        val progress = fill * rows.size
        val row = progress.toInt().coerceAtMost(rows.lastIndex)
        val forward = row % 2 == 0
        val step = bounds.height()/rows.size
        val y = bounds.top + row*step
        zigzag(index,progress,tip)
        c.save()
        c.skew(0f,tilt)
        c.clipPath(path)
        solid(selected!!)
        c.drawRect(bounds.left-1, bounds.top-1, bounds.right+1, y, paint)
        // The completed portion of each row meets the row above without gaps.
        val start = if (forward) bounds.left-1 else bounds.right+1
        if (progress-row >= .84f && row < rows.lastIndex)
            c.drawRect(bounds.left-1,y-.5f,bounds.right+1,y+step,paint)
        c.drawRect(minOf(start,tip[0]),y-.5f,maxOf(start,tip[0]),y+step,paint)
        c.drawCircle(tip[0],tip[1],step/2,paint)
        tip[1] += tip[0]*tilt
        c.restore()
    }

    fun guidePoint(t: Float, out: PointF) {
        val tracing = freehand || outline < 1f
        val progress = if (tracing) (if (freehand) 0f else outline) + .09f*t else fill + 2*t/lanes[colors.size].size
        pointAt(progress,tracing,out)
    }

    private fun pointAt(value: Float, tracing: Boolean, out: PointF) {
        if (tracing) {
            var distance = value.coerceIn(0f,1f)*totalLength
            var index = 0
            while (index < lengths.lastIndex && distance >= lengths[index]) distance -= lengths[index++]
            measure.setPath(picture.outlines[index],false)
            measure.getPosTan(distance,guidePosition,null)
            out.set(left+guidePosition[0]*scale,top+guidePosition[1]*scale)
        } else if (!done) {
            val index = colors.size
            val rows = lanes[index]
            val progress = (value*rows.size).coerceAtMost(rows.size-.001f)
            paintPaths[index].computeBounds(bounds,true)
            zigzag(index,progress,guidePosition)
            out.set(left+guidePosition[0]*scale,top+(guidePosition[1]+guidePosition[0]*tilt)*scale)
        }
    }
    private val guidePosition = FloatArray(2)

    private fun face(c: Canvas) {
        val y = when (page) { 0 -> 140f; 1 -> 173f; else -> 223f }
        val x = 160f
        val size = if (page == 2) .7f else 1f
        c.save(); c.translate(x,y); c.scale(size,size)
        for (side in listOf(-1,1)) {
            solid(ink); c.drawOval(side*18f-2.8f,-4f,side*18f+2.8f,3f,paint)
            solid(0xfffea6a8.toInt()); c.drawOval(side*25f-5f,2f,side*25f+5f,7f,paint)
        }
        line(ink,2.6f); c.drawArc(-6f,-2f,6f,8f,8f,164f,false,paint)
        c.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lastTime >= 0) {
            val now = SystemClock.uptimeMillis()
            val dt = (now - lastTime).coerceIn(0, 80)
            lastTime = now
            advance(dt)
        }
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        picture.regions.forEachIndexed { i, path ->
            solid(if (i < colors.size) colors[i] else if (!freehand && outline == 1f && i == colors.size) 0xffffefcf.toInt() else Color.WHITE)
            canvas.drawPath(path, paint)
            if (!freehand && i == colors.size && fill > 0f) {
                paintRegion(canvas,paintPaths[i],i)
            }
        }
        if (freehand || outline == 1f) picture.drawMarks(canvas,paint)
        line(0xffada0bd.toInt(), 2.6f)
        if (!freehand) paint.pathEffect = dotted
        picture.outlines.forEach { canvas.drawPath(it, paint) }
        paint.pathEffect = null
        if (freehand) {
            val layer = canvas.saveLayer(null,null)
            strokes.forEach {
                line(it.color, if (it.erase) 19f else 5.5f)
                paint.xfermode = if (it.erase) clearInk else null
                if (it.moved) canvas.drawPath(it.path, paint)
                else canvas.drawPoint(it.x, it.y, paint)
            }
            paint.xfermode = null
            canvas.restoreToCount(layer)
        } else {
            line(ink, 4.8f)
            var remaining = outline * totalLength
            picture.outlines.forEachIndexed { i, path ->
                if (outline == 1f || remaining >= lengths[i]) canvas.drawPath(path, paint)
                else if (remaining > 0f) {
                    measure.setPath(path, false)
                    measure.getPosTan(remaining,tip,null)
                    segment.reset()
                    measure.getSegment(0f, remaining, segment, true)
                    canvas.drawPath(segment, paint)
                }
                remaining -= lengths[i]
            }
        }
        if (!freehand) {
            if (outline == 1f && page < 3) face(canvas)
            if (done) {
                for (i in 0..3) {
                    canvas.save()
                    canvas.translate(if (i < 2) 45f + i*14 else 260f + (i-2)*14,
                        when (i) { 0 -> 105f; 1 -> 86f; 2 -> 175f; else -> 193f })
                    val size = if (i % 2 == 0) 1.15f else .55f
                    canvas.scale(size,size)
                    solid(palette[(page+i)%palette.size].second)
                    canvas.drawPath(sparkle,paint)
                    canvas.restore()
                }
            }
            if (outline < 1f || selected != null) {
                if (outline == 0f) {
                    measure.setPath(picture.outlines.first(),false)
                    measure.getPosTan(0f,tip,null)
                } else if (outline == 1f && fill == 0f) {
                    paintPaths[colors.size].computeBounds(bounds,true)
                    tip[0] = lanes[colors.size].first().left
                    tip[1] = bounds.top + bounds.height()/lanes[colors.size].size/2f + tip[0]*tilt
                }
                drawCrayon(canvas,selected ?: palette.last().second)
            }
        }
        canvas.restore()
        if (lastTime >= 0) postInvalidateOnAnimation()
    }
}
