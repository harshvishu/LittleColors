package family.littlecolors

import android.app.Activity
import android.app.ActivityManager
import android.os.*
import android.media.AudioManager
import android.media.MediaPlayer
import family.littlecolors.DrawingBoard.Mode
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import kotlin.math.min

class PlayActivity : Activity() {
    private val cream = 0xfff5f0fa.toInt()
    private val paper = 0xfffffdf8.toInt()
    private val green = 0xff655099.toInt()
    private lateinit var sounds: PlaySounds
    private lateinit var board: DrawingBoard
    private lateinit var root: LinearLayout
    private lateinit var stage: FrameLayout
    private lateinit var confetti: Confetti
    private lateinit var guide: FingerGuide
    private var picker: FrameLayout? = null
    private var guideTarget: View? = null
    private var touching = false
    private val screenPos = IntArray(2)
    private val stagePos = IntArray(2)
    private lateinit var footer: LinearLayout
    private var playing = false
    private var resumed = false
    private var musicEnabled = true
    private var music: MediaPlayer? = null
    private var shelf = 0

    private fun syncMusic() {
        if (playing && resumed && musicEnabled && sounds.enabled) {
            val player = music ?: MediaPlayer.create(this,R.raw.lullaby)?.also {
                it.isLooping = true
                it.setVolume(.20f,.20f)
                music = it
            }
            if (player != null && !player.isPlaying) player.start()
        } else music?.let { if (it.isPlaying) it.pause() }
    }
    private var session = false
    private var waitingPin = false
    private var pinAttempt = false
    private val handler = Handler(Looper.getMainLooper())
    private val pinned get() = getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val prefs = getSharedPreferences("play", MODE_PRIVATE)
        volumeControlStream = AudioManager.STREAM_MUSIC
        sounds = PlaySounds(this).apply { enabled = prefs.getBoolean("sounds", true) }
        board = DrawingBoard(this, { cue ->
            sounds.play(cue)
            if (playing && cue != Cue.TOUCH) confetti.burst(cue == Cue.FINISH)
        }, sounds::pencil) { if (playing) refreshPlay() }
        musicEnabled = prefs.getBoolean("music",true)
        val oldMode = if (prefs.getBoolean("free",false)) Mode.FREEHAND else Mode.ASSISTED
        val mode = Mode.values().firstOrNull { it.name == prefs.getString("mode",null) } ?: oldMode
        board.open(prefs.getInt("page", 0).coerceIn(pictures.indices), mode)
        shelf = board.page/10
        parentScreen()
    }

    @Suppress("DEPRECATION")
    private fun fullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onWindowFocusChanged(focus: Boolean) {
        super.onWindowFocusChanged(focus)
        if (focus) {
            fullscreen()
            checkPin()
        } else if (::board.isInitialized) {
            board.cancelTouch()
            touching = false
            if (playing) guide.touch(false)
        }
    }

    // Some Android pin confirmations overlay the activity without changing its focus.
    private val pollPin = object : Runnable {
        override fun run() {
            checkPin()
            if (waitingPin) handler.postDelayed(this, 350)
        }
    }

    override fun onStart() {
        super.onStart()
        if (waitingPin) handler.post(pollPin)
    }

    override fun onStop() {
        handler.removeCallbacks(pollPin)
        super.onStop()
    }

    private fun checkPin() {
        if (waitingPin && pinned) {
            waitingPin = false
            play()
        }
    }

    private fun requestPin() {
        if (pinned) { play(); return }
        pinAttempt = true
        waitingPin = true
        parentScreen()
        try { startLockTask() } catch (_: IllegalStateException) { waitingPin = false }
        handler.removeCallbacks(pollPin)
        if (waitingPin) handler.postDelayed(pollPin, 350)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun rounded(color: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(2), border)
    }

    private fun label(value: String, size: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(ink)
        gravity = Gravity.CENTER
        typeface = Typeface.create(if (bold) "sans-serif-rounded" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 16f
        setTextColor(if (primary) Color.WHITE else ink)
        backgroundTintList = null
        background = rounded(if (primary) green else 0xffe9dff6.toInt(), 18)
        stateListAnimator = null
        isSoundEffectsEnabled = false
        setPadding(dp(14), 0, dp(14), 0)
        setOnClickListener { action() }
    }

    private fun controls() = LinearLayout(this).apply {
        gravity = Gravity.CENTER; isMotionEventSplittingEnabled = false
    }

    private fun LinearLayout.item(v: View, w: Int = -2, h: Int = 50, gap: Int = 4) {
        addView(v, LinearLayout.LayoutParams(if (w < 0) w else dp(w), if (h < 0) h else dp(h)).apply {
            setMargins(dp(gap), dp(3), dp(gap), dp(3))
        })
    }

    private fun layout() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8)); setBackgroundColor(cream)
        isMotionEventSplittingEnabled = false
    }

    private fun choose(page: Int, mode: Mode) {
        board.open(page, mode)
        shelf = page/10
        getSharedPreferences("play", MODE_PRIVATE).edit().putInt("page",page).putString("mode",mode.name).apply()
    }

    private fun parentScreen() {
        playing = false
        syncMusic()
        board.cancelTouch()
        (board.parent as? ViewGroup)?.removeView(board)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = layout()
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isMotionEventSplittingEnabled = false }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(panel) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,-1))
        setContentView(root)
        panel.item(label(if (session) "Parent menu" else "Little Colors", 34, true), h = 54)
        val nav = controls()
        nav.item(button("‹") { shelf--; parentScreen() }.apply { isEnabled = shelf > 0; contentDescription = "Previous ten pictures" },52)
        nav.item(label("Levels ${shelf*10+1}–${shelf*10+10}",18,true))
        nav.item(button("›") { shelf++; parentScreen() }.apply { isEnabled = shelf < pictures.lastIndex/10; contentDescription = "Next ten pictures" },52)
        panel.item(nav,h=-2)
        var pages = controls()
        val roomy = resources.configuration.screenWidthDp >= 600
        val columns = if (roomy) 5 else 3
        for (i in shelf*10 until min((shelf+1)*10,pictures.size)) {
            if ((i%10)%columns == 0) {
                pages = controls(); panel.item(pages,h=-2)
            }
            val pic = pictures[i]
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                contentDescription = pic.name
                background = rounded(if (i == board.page) 0xffe9dff6.toInt() else paper, 16, if (i == board.page) green else cream)
                addView(object : View(this@PlayActivity) {
                    private val pen = Paint(Paint.ANTI_ALIAS_FLAG)
                    override fun onDraw(c: Canvas) {
                        val s = min(width / 320f, height / 300f)
                        c.save(); c.translate((width-320*s)/2,(height-300*s)/2); c.scale(s,s)
                        pic.regions.forEachIndexed { n, p ->
                            pen.style = Paint.Style.FILL; pen.color = pic.previewColors.getOrNull(n) ?: palette[(i+n)%palette.size].second; c.drawPath(p,pen)
                        }
                        pic.drawMarks(c,pen)
                        pen.style = Paint.Style.STROKE; pen.color = ink; pen.strokeWidth = 4.8f; pen.strokeJoin = Paint.Join.ROUND
                        pic.outlines.forEach { c.drawPath(it,pen) }; c.restore()
                    }
                }, LinearLayout.LayoutParams(-1, dp(if (roomy) 96 else 62)))
                addView(label(pic.name, 14, true))
                setOnClickListener { if (i != board.page) choose(i, board.mode); parentScreen() }
            }
            pages.item(tile, if (roomy) 103 else 84, if (roomy) 136 else 98)
        }
        val modes = controls()
        for ((mode,name) in listOf(Mode.ASSISTED to "Assisted",Mode.FOLLOW to "Follow Along",Mode.FREEHAND to "Freehand")) {
            modes.item(button(name,board.mode == mode) {
                if (mode != board.mode) choose(board.page,mode)
                parentScreen()
            }.apply { setPadding(dp(8),0,dp(8),0); textSize = 15f })
        }
        panel.item(modes, h = -2)
        panel.item(button(if (sounds.enabled) "♫  Sound on" else "Sound off") {
            sounds.enabled = !sounds.enabled
            getSharedPreferences("play", MODE_PRIVATE).edit().putBoolean("sounds",sounds.enabled).apply()
            if (sounds.enabled) sounds.play(Cue.SWATCH)
            parentScreen()
        }, 220)
        panel.item(button(if (musicEnabled) "Music on" else "Music off") {
            musicEnabled = !musicEnabled
            getSharedPreferences("play",MODE_PRIVATE).edit().putBoolean("music",musicEnabled).apply()
            parentScreen()
        },220)
        val status = when {
            pinned -> "App is pinned"
            pinAttempt -> "Play is unpinned. Confirm Android’s prompt or check your pinning settings."
            else -> "Start play to pin the app to the screen."
        }
        panel.item(label(status, 15), h = -2)
        panel.item(button(if (session && pinned) "Resume play" else "Start play", true) { requestPin() }, 220)
        if (pinAttempt && !pinned) panel.item(button("Play without pinning") { waitingPin = false; play() }, 220)
        panel.item(label("Vector illustrations adapted from Twemoji · CC BY 4.0",12),h=-2)
        if (session) panel.item(button("Exit play", action = ::closePlay), 220)
    }

    private fun play() {
        session = true; playing = true
        syncMusic()
        root = layout()
        val header = controls()
        val badge = label("★  " + (board.page + 1) + " / " + pictures.size, 16, true)
        badge.background = rounded(0xffffdf8e.toInt(),18)
        badge.contentDescription = "Picture ${board.page+1} of ${pictures.size}"
        header.item(homeButton(),54,48)
        header.item(badge,84,38)
        val name = label(pictures[board.page].name, 27, true)
        header.addView(name, LinearLayout.LayoutParams(0,dp(54),1f))
        val exit = button("×") {}
        exit.textSize = 26f
        exit.contentDescription = "Hold five seconds to close app"
        bindHold(exit,5000L,::closePlay)
        header.item(exit, 54, 48)
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        board.background = rounded(paper, 26, 0xffe0d6ea.toInt())
        (board.parent as? ViewGroup)?.removeView(board)
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        footer = controls()
        root.item(footer, w = -1, h = 78)
        picker = null
        stage = FrameLayout(this).apply { isMotionEventSplittingEnabled = false; addView(root) }
        guide = FingerGuide(this, ::locateFinger)
        stage.addView(guide)
        confetti = Confetti(this)
        stage.addView(confetti)
        if (touching) guide.touch(true)
        setContentView(stage)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fullscreen()
        refreshPlay()
    }

    private fun closePlay() {
        waitingPin = false
        board.cancelTouch()
        sounds.stop()
        if (pinned) stopLockTask()
        finishAndRemoveTask()
    }

    private fun homeButton() = button("⌂") {}.apply {
        textSize = 27f
        contentDescription = "Hold two seconds for home"
        bindHold(this,2000L) { shelf = board.page/10; parentScreen() }
    }

    private fun bindHold(exit: Button, duration: Long, action: () -> Unit) {
        val idle = exit.text
        var started = -1L
        val tick = object : Runnable {
            override fun run() {
                if (started < 0 || !exit.isShown || !exit.hasWindowFocus()) return
                val left = duration - (SystemClock.uptimeMillis() - started)
                if (left <= 0) { started = -1; action(); return }
                exit.text = ((left+999)/1000).toString()
                exit.postDelayed(this,minOf(left,100L))
            }
        }
        fun reset() { started = -1; exit.removeCallbacks(tick); exit.text = idle }
        exit.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { started = e.eventTime; tick.run() }
                MotionEvent.ACTION_MOVE -> if (e.x !in 0f..exit.width.toFloat() || e.y !in 0f..exit.height.toFloat()) reset()
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> reset()
                MotionEvent.ACTION_UP -> {
                    val complete = started >= 0 && e.eventTime-started >= duration && !isFinishing
                    reset()
                    if (complete) action()
                }
            }
            true
        }
    }

    private fun refreshPlay() {
        board.contentDescription = pictures[board.page].name + ". " + board.hint
        picker?.let { stage.removeView(it) }
        picker = null
        guideTarget = null
        root.visibility = if (board.choosing) View.INVISIBLE else View.VISIBLE
        footer.removeAllViews()
        footer.visibility = if (board.freehand || board.done) View.VISIBLE else View.GONE
        if (board.choosing) {
            openPicker()
        } else if (board.freehand) {
            val available = resources.configuration.screenWidthDp - 34
            val colors = pictures[board.page].swatches
            val swatch = ((available / (colors.size+1)) - 4).coerceIn(28,72)
            colors.forEach { (name, color) ->
                val dot = button(if (!board.erasing && board.selected == color) "✓" else "") { board.pick(color) }
                dot.contentDescription = name
                dot.textSize = 24f
                dot.background = rounded(color, 17, ink)
                dot.isSelected = !board.erasing && board.selected == color
                dot.setTextColor(if (Color.luminance(color) < .38f) Color.WHITE else ink)
                dot.setPadding(0,0,0,0)
                footer.item(dot, swatch, min(swatch,64),2)
            }
            val eraser = button("▱",board.erasing) { board.erase() }.apply {
                contentDescription = "Eraser"; isSelected = board.erasing; textSize = 24f; setPadding(0,0,0,0)
            }
            footer.item(eraser,swatch,min(swatch,64),2)
        } else if (board.done) {
            val next = button("➜", true) {
                choose((board.page+1) % pictures.size, board.mode)
                play()
            }.apply {
                textSize = 28f
                contentDescription = if (board.page == pictures.lastIndex) "Play again" else "Next picture"
            }
            footer.item(next,100,64)
            guideTarget = next
        }
        guide.step()
    }

    private fun openPicker() {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(paper)
            isMotionEventSplittingEnabled = false
            isClickable = true
        }
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isMotionEventSplittingEnabled = false
            setPadding(dp(8),dp(8),dp(8),dp(8))
        }
        val columns = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        for (group in pictures[board.page].swatches.chunked(columns)) {
            val row = controls()
            for ((name,color) in group) {
                val tile = button("") { board.pick(color) }.apply {
                    contentDescription = name
                    background = rounded(color,24,ink)
                }
                row.addView(tile,LinearLayout.LayoutParams(0,-1,1f).apply { setMargins(dp(5),dp(5),dp(5),dp(5)) })
                if (guideTarget == null) guideTarget = tile
            }
            grid.addView(row,LinearLayout.LayoutParams(-1,0,1f))
        }
        overlay.addView(grid)
        overlay.addView(homeButton(),FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP or Gravity.START).apply {
            topMargin = dp(16); marginStart = dp(16)
        })
        val exit = button("×") {}.apply { contentDescription = "Hold five seconds to close app" }
        bindHold(exit,5000L,::closePlay)
        overlay.addView(exit,FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP or Gravity.END).apply {
            topMargin = dp(16); marginEnd = dp(16)
        })
        picker = overlay
        stage.addView(overlay,stage.indexOfChild(guide))
    }

    private fun locateFinger(t: Float, out: PointF): Boolean {
        val target = guideTarget ?: board
        if (!target.isShown || target.width <= 0) return false
        target.getLocationInWindow(screenPos)
        stage.getLocationInWindow(stagePos)
        if (target == board) board.guidePoint(t,out)
        else out.set(target.width*.5f,target.height*.5f - dp(12)*(1f-t))
        out.offset((screenPos[0]-stagePos[0]).toFloat(),(screenPos[1]-stagePos[1]).toFloat())
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touching = true
            if (playing) guide.touch(true)
        }
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touching = false
            if (playing) guide.touch(false)
        }
        return handled
    }

    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        super.onConfigurationChanged(config)
        if (playing && board.choosing) refreshPlay()
    }

    override fun onResume() { super.onResume(); resumed = true; syncMusic() }
    override fun onPause() { resumed = false; syncMusic(); sounds.stop(); board.cancelTouch(); super.onPause() }
    override fun onDestroy() { music?.release(); sounds.release(); handler.removeCallbacksAndMessages(null); super.onDestroy() }
    @Deprecated("Legacy back dispatch is explicitly enabled for pinned play")
    override fun onBackPressed() { if (!playing && !session) super.onBackPressed() }
}
