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
    private var followTarget = 0f
    private var lastFollowFrame = -1L
    val guidesFollowing get() = mode == Mode.FOLLOW && !showingSymbol && !choosing && !done
    private val followRadius = 54f
    private val followLead = 32f
    private val pencilUnitsPerMillisecond = .15f
    private val picture get() = pictures[page]
    private var lessonStrokes: List<Path>? = null
    private val outlines get() = lessonStrokes ?: picture.outlines
    val traceComplete get() = lessonStrokes != null && outline == 1f
    private var symbolFadeStart = 0L
    var showingSymbol = false
        set(value) {
            if (field && !value) symbolFadeStart = SystemClock.uptimeMillis()
            field = value
            invalidate()
        }
    private val lessonInk = 0xff7353ad.toInt()
    private var outline = 0f
    private var fill = 0f
    private val colors = mutableListOf<Int>()
    var selected: Int? = null
        private set
    val choosing get() = lessonStrokes == null && !freehand && outline == 1f && !done && selected == null
    val done get() = if (lessonStrokes == null) !freehand && colors.size == picture.regions.size else traceComplete
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
    private var lengths = outlines.map { PathMeasure(it, false).length }
    private var strokeDistances = lengths.runningFold(0f) { sum,length -> sum + length }
    private val totalLength get() = strokeDistances.last()
    private val scale get() = min(width / 300f, height / 288f).coerceAtLeast(.001f)
    private val left get() = (width - 320 * scale) / 2
    private val top get() = (height - 300 * scale) / 2

    private val dotted = DashPathEffect(floatArrayOf(.4f, 6.2f), 0f)
    private val tip = FloatArray(2)
    private val wood = Path().apply { moveTo(0f,0f); lineTo(9f,-4.8f); lineTo(9f,4.8f); close() }
    private val nib = Path().apply { moveTo(0f,0f); lineTo(3.6f,-2f); lineTo(3.6f,2f); close() }
    private data class Lane(val left: Float, val right: Float)

    // The zigzag uses more of its progress range for horizontal sweeps than turns.
    // Map that range to distances on the visible, skewed path for Assisted play.
    private class FillDistance(rows: List<Lane>, step: Float, tilt: Float) {
        private val ends = FloatArray(rows.size * 2)
        private val lastRow = rows.lastIndex
        init {
            rows.forEachIndexed { row, lane ->
                val width = lane.right - lane.left
                val sweepEnd = row * 2 + 1
                ends[sweepEnd] = ends[sweepEnd - 1] + kotlin.math.hypot(width, width * tilt)
                if (row < lastRow) {
                    val next = rows[row + 1]
                    val dx = if (row % 2 == 0) next.right - lane.right else next.left - lane.left
                    ends[sweepEnd + 1] = ends[sweepEnd] + kotlin.math.hypot(dx, step + dx * tilt)
                }
            }
        }

        val length get() = ends.last()

        fun at(progress: Float): Float {
            val location = progress * (lastRow + 1)
            val row = location.toInt().coerceAtMost(lastRow)
            val local = location - row
            val sweepEnd = if (row == lastRow) 1f else SWEEP_END
            val segment = row * 2 + if (local <= sweepEnd) 0 else 1
            val fraction = if (local <= sweepEnd) local / sweepEnd else (local - sweepEnd) / (1f - sweepEnd)
            return ends[segment] + (ends[segment + 1] - ends[segment]) * fraction
        }

        fun progressAt(distance: Float): Float {
            if (distance >= length) return 1f
            var next = 0
            while (distance >= ends[next + 1]) next++
            val along = (distance - ends[next]) / (ends[next + 1] - ends[next])
            val position = when {
                next / 2 == lastRow -> along
                next % 2 == 0 -> along * SWEEP_END
                else -> SWEEP_END + along * (1f - SWEEP_END)
            }
            return (next / 2 + position) / (lastRow + 1)
        }
    }

    private companion object {
        const val SWEEP_END = .84f
    }
    private val tilt = .28f
    private val slant = Matrix().apply { setSkew(0f,-tilt) }
    private var paintPaths = emptyList<Path>()
    private val sparkle = Path().apply {
        moveTo(0f,-9f); quadTo(1f,-1f,7f,0f); quadTo(1f,1f,0f,9f)
        quadTo(-1f,1f,-7f,0f); quadTo(-1f,-1f,0f,-9f); close()
    }
    private var lanes = emptyList<List<Lane>>()
    private var fillDistances = emptyList<FillDistance>()

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
        lessonStrokes = null
        page = index
        mode = newMode
        erasing = false
        outline = 0f
        fill = 0f
        colors.clear()
        strokes.clear()
        selected = if (freehand) picture.swatches.first().second else null
        paintPaths = picture.regions.map { Path(it).apply { transform(slant) } }
        lanes = paintPaths.map(::paintLanes)
        fillDistances = paintPaths.zip(lanes).map { (path, rows) ->
            path.computeBounds(bounds, true)
            FillDistance(rows, bounds.height() / rows.size, tilt)
        }
        measureOutlines()
        invalidate()
        changed()
    }

    fun openTrace(strokePaths: List<Path>, tracingMode: Mode) {
        cancelTouch()
        lessonStrokes = strokePaths
        mode = tracingMode
        outline = 0f
        selected = null
        showingSymbol = true
        measureOutlines()
    }

    private fun measureOutlines() {
        lengths = outlines.map { PathMeasure(it, false).length }
        strokeDistances = lengths.runningFold(0f) { sum,length -> sum + length }
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
        lastFollowFrame = -1
        followTarget = 0f
    }

    private fun advance(ms: Long) {
        val distance = ms * pencilUnitsPerMillisecond
        if (outline < 1f) advanceFraction(outline + distance / totalLength)
        else if (selected != null && !done) {
            val route = fillDistances[colors.size]
            advanceFraction(route.progressAt(route.at(fill) + distance))
        }
    }

    private fun advanceFraction(value: Float) {
        if (outline < 1f) {
            outline = min(1f,value)
            if (outline == 1f) {
                cancelTouch()
                if (lessonStrokes == null) sound(Cue.READY)
                changed()
            }
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
        if (lessonStrokes != null && (showingSymbol || traceComplete)) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (freehand || (!choosing && !done)) sound(Cue.TOUCH)
                if (freehand) {
                    pointer = e.getPointerId(0)
                    val x = (e.x - left) / scale
                    val y = (e.y - top) / scale
                    lastX = e.x; lastY = e.y
                    strokes.add(Stroke(selected!!, erasing, Path().apply { moveTo(x, y) }, x, y))
                } else if (mode == Mode.FOLLOW && !choosing && !done) {
                    pointer = e.getPointerId(0)
                    lastX = e.x; lastY = e.y
                } else if (!choosing && !done) {
                    lastTime = SystemClock.uptimeMillis()
                    pencilSound(true)
                    advance(45)
                }
            }
            MotionEvent.ACTION_MOVE -> if (freehand) {
                val i = e.findPointerIndex(pointer)
                if (i >= 0) {
                    val stroke = strokes.last()
                    val x = e.getX(i)
                    val y = e.getY(i)
                    if (!stroke.erase && kotlin.math.hypot(x-lastX,y-lastY)/scale >= .20f) {
                        playMovingPencil()
                        lastX = x; lastY = y
                    }
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
            MotionEvent.ACTION_POINTER_UP -> if (pointer == e.getPointerId(e.actionIndex)) liftFinger()
            MotionEvent.ACTION_UP -> liftFinger()
            MotionEvent.ACTION_CANCEL -> cancelTouch()
        }
        invalidate()
        return true
    }

    private val hushPencil = Runnable { pencilSound(false) }

    private fun playMovingPencil() {
        pencilSound(true)
        removeCallbacks(hushPencil)
        postDelayed(hushPencil,160)
    }

    private fun liftFinger() {
        if (mode == Mode.FOLLOW && lastFollowFrame >= 0) pointer = -1
        else cancelTouch()
    }

    private fun strokeIndex(distance: Float): Int {
        for (i in lengths.indices) if (distance < strokeDistances[i+1]) return i
        return lengths.lastIndex
    }

    private fun follow(x: Float, y: Float) {
        if (pointer < 0 || !guidesFollowing) return
        val speedScale = scale
        val travel = kotlin.math.hypot(x-lastX,y-lastY) / speedScale
        if (travel < .20f) return
        lastX = x
        lastY = y
        val tracing = outline < 1f
        val route = if (tracing) null else fillDistances[colors.size]
        val drawn = if (route == null) outline * totalLength else route.at(fill)
        val from = maxOf(drawn,followTarget)
        val stroke = if (tracing) strokeIndex(from) else 0
        val end = route?.length ?: strokeDistances[stroke+1]
        val limit = minOf(end, from + travel * 1.65f, drawn + followLead)
        if (limit <= from) return

        if (tracing) measure.setPath(outlines[stroke],false)
        fun locate(distance: Float) {
            if (route == null) {
                // Keep the endpoint within this stroke when the next one starts elsewhere.
                measure.getPosTan(distance-strokeDistances[stroke],guidePosition,null)
                cursor.set(left+guidePosition[0]*speedScale,top+guidePosition[1]*speedScale)
            } else pointAt(route.progressAt(distance),false,cursor)
        }

        locate(from)
        var previousX = cursor.x
        var previousY = cursor.y
        var bestError = kotlin.math.hypot(x-previousX,y-previousY)
        if (bestError > followRadius * speedScale) return
        var best = from
        var segmentStart = from
        while (segmentStart < limit) {
            val candidate = minOf(segmentStart + 2f,limit)
            locate(candidate)
            val dx = cursor.x - previousX
            val dy = cursor.y - previousY
            val squared = dx*dx + dy*dy
            if (squared > 0f) {
                val fraction = (((x-previousX)*dx + (y-previousY)*dy) / squared).coerceIn(0f,1f)
                val error = kotlin.math.hypot(x-previousX-fraction*dx,y-previousY-fraction*dy)
                if (error < bestError) {
                    bestError = error
                    best = segmentStart + (candidate-segmentStart)*fraction
                }
            }
            segmentStart = candidate
            previousX = cursor.x
            previousY = cursor.y
        }
        // Tiny inaccuracies near an endpoint should never leave the stroke unfinished.
        if (end-best < 3f && best > from) best = end
        if (best <= from) return
        followTarget = best
        if (lastFollowFrame < 0) lastFollowFrame = SystemClock.uptimeMillis()
        playMovingPencil()
    }

    private fun animateFollow(now: Long) {
        val dt = (now - lastFollowFrame).coerceIn(0, 50)
        lastFollowFrame = now
        val tracing = outline < 1f
        val route = if (tracing) null else fillDistances[colors.size]
        val drawn = if (route == null) outline * totalLength else route.at(fill)
        val pending = (followTarget - drawn).coerceAtLeast(0f)
        val speed = .24f + pending * .021f
        val next = minOf(followTarget,drawn + dt * speed)
        if (next <= drawn && followTarget > drawn) return
        if (next >= followTarget) {
            lastFollowFrame = -1
            if (pointer < 0) pencilSound(false)
        }
        if (next > drawn) advanceFraction(if (route == null) next / totalLength else route.progressAt(next))
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
        val sweep = if (row == rows.lastIndex) 1f else SWEEP_END
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
        if (progress-row >= SWEEP_END && row < rows.lastIndex)
            c.drawRect(bounds.left-1,y-.5f,bounds.right+1,y+step,paint)
        c.drawRect(minOf(start,tip[0]),y-.5f,maxOf(start,tip[0]),y+step,paint)
        c.drawCircle(tip[0],tip[1],step/2,paint)
        tip[1] += tip[0]*tilt
        c.restore()
    }

    fun guidePoint(t: Float, out: PointF) {
        val tracing = freehand || outline < 1f
        val progress = if (guidesFollowing) {
            val lead = 16f + 9f * t
            if (tracing) {
                val drawn = outline * totalLength
                minOf(strokeDistances[strokeIndex(drawn)+1] - .001f, drawn + lead) / totalLength
            } else {
                val route = fillDistances[colors.size]
                route.progressAt(route.at(fill) + lead)
            }
        } else if (tracing) {
            (if (freehand) 0f else outline) + .09f*t
        } else fill + 2*t/lanes[colors.size].size
        pointAt(progress,tracing,out)
    }

    private fun pointAt(value: Float, tracing: Boolean, out: PointF) {
        if (tracing) {
            val distance = value.coerceIn(0f,1f)*totalLength
            val index = strokeIndex(distance)
            measure.setPath(outlines[index],false)
            measure.getPosTan(distance-strokeDistances[index],guidePosition,null)
            out.set(left+guidePosition[0]*scale,top+guidePosition[1]*scale)
        } else if (!done) {
            val index = colors.size
            val rows = lanes[index]
            val progress = (value*rows.size).coerceAtMost(rows.size.toFloat())
            paintPaths[index].computeBounds(bounds,true)
            zigzag(index,progress,guidePosition)
            out.set(left+guidePosition[0]*scale,top+(guidePosition[1]+guidePosition[0]*tilt)*scale)
        }
    }
    private val guidePosition = FloatArray(2)

    private fun drawOutlineProgress(canvas: Canvas, color: Int, weight: Float) {
        line(color, weight)
        var remaining = outline * totalLength
        outlines.forEachIndexed { i, path ->
            if (outline == 1f || remaining >= lengths[i]) {
                canvas.drawPath(path, paint)
            } else if (remaining > 0f || (remaining == 0f && (lessonStrokes != null || mode == Mode.FOLLOW))) {
                measure.setPath(path, false)
                measure.getPosTan(remaining, tip, null)
                segment.reset()
                if (measure.getSegment(0f, remaining, segment, true)) canvas.drawPath(segment, paint)
            }
            remaining -= lengths[i]
        }
    }

    private fun drawLessonTrace(canvas: Canvas) {
        if (showingSymbol) {
            line(lessonInk, 15f)
            outlines.forEach { canvas.drawPath(it, paint) }
            return
        }
        line(0xffaaa0c0.toInt(), 4f)
        paint.pathEffect = dotted
        outlines.forEach { canvas.drawPath(it, paint) }
        paint.pathEffect = null
        drawOutlineProgress(canvas, lessonInk, 12f)
        val elapsed = SystemClock.uptimeMillis() - symbolFadeStart
        if (elapsed < 180) {
            line(lessonInk, 15f)
            paint.alpha = ((1f - elapsed / 180f) * 255).toInt()
            outlines.forEach { canvas.drawPath(it, paint) }
            paint.alpha = 255
            postInvalidateOnAnimation()
        }
        if (!traceComplete) drawCrayon(canvas, lessonInk)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lastFollowFrame >= 0) animateFollow(SystemClock.uptimeMillis())
        if (lastTime >= 0) {
            val now = SystemClock.uptimeMillis()
            val dt = (now - lastTime).coerceIn(0, 80)
            lastTime = now
            advance(dt)
        }
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        if (lessonStrokes != null) {
            drawLessonTrace(canvas)
            canvas.restore()
            if (lastTime >= 0 || lastFollowFrame >= 0) postInvalidateOnAnimation()
            return
        }
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
            drawOutlineProgress(canvas, ink, 4.8f)
        }
        if (!freehand) {
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
                drawCrayon(canvas,selected ?: picture.swatches.first().second)
            }
        }
        canvas.restore()
        if (lastTime >= 0 || lastFollowFrame >= 0) postInvalidateOnAnimation()
    }
}
