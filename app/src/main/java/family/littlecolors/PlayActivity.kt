package family.littlecolors

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import family.littlecolors.DrawingBoard.Mode
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.util.Locale
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
    private val lessons by lazy { loadLearningBooks(this) }
    private val preferences by lazy { getSharedPreferences("play", MODE_PRIVATE) }
    private var learningSelected = false
    private var learningBook = LearningBook.ABC
    private var learningIndex = 0
    private var learningMode = Mode.ASSISTED
    private val lesson get() = lessons.getValue(learningBook)[learningIndex]
    private var revealVisible = false
    private var lessonCard: FrameLayout? = null
    private val lessonBoard: DrawingBoard by lazy {
        DrawingBoard(this, sounds::play, sounds::pencil) {
            if (playing && learningSelected && lessonBoard.traceComplete) handler.post(::revealLesson)
        }
    }
    private val activeBoard get() = if (learningSelected) lessonBoard else board
    private val narration by lazy {
        LessonNarration(this) { speaking ->
            val level = if (speaking) .05f else .20f
            music?.setVolume(level,level)
        }
    }
    private val speakPictureName = Runnable {
        if (playing && resumed && !learningSelected && board.done && sounds.enabled) {
            val name = pictures[board.page].name.lowercase(Locale.ROOT).replace(' ', '_')
            narration.play("coloring/speech/$name.wav")
        }
    }
    private val endIntroduction = Runnable {
        if (playing && learningSelected && lessonBoard.showingSymbol) {
            lessonBoard.showingSymbol = false
            lessonBoard.contentDescription = "Trace ${lesson.symbol}"
            guide.visibility = View.VISIBLE
            guide.step()
        }
    }

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
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
        val prefs = preferences
        volumeControlStream = AudioManager.STREAM_MUSIC
        sounds = PlaySounds(this).apply { enabled = prefs.getBoolean("sounds", true) }
        board = DrawingBoard(this, { cue ->
            sounds.play(cue)
            if (playing && cue != Cue.TOUCH) {
                confetti.burst(cue == Cue.FINISH)
                // Let the chime fade before saying the name.
                if (cue == Cue.FINISH && sounds.enabled) handler.postDelayed(speakPictureName, 650L)
            }
        }, sounds::pencil) { if (playing) refreshPlay() }
        musicEnabled = prefs.getBoolean("music",true)
        val oldMode = if (prefs.getBoolean("free",false)) Mode.FREEHAND else Mode.ASSISTED
        val mode = Mode.values().firstOrNull { it.name == prefs.getString("mode",null) } ?: oldMode
        board.open(prefs.getInt("page", 0).coerceIn(pictures.indices), mode)
        shelf = board.page/10
        learningSelected = prefs.getBoolean("learning.selected", false)
        learningBook = if (prefs.getString("learning.book", null) == LearningBook.NUMBERS.key) LearningBook.NUMBERS else LearningBook.ABC
        learningIndex = lastLesson(learningBook)
        if (prefs.getBoolean("learning.follow", false)) learningMode = Mode.FOLLOW
        parentScreen()
    }

    @Suppress("DEPRECATION")
    private fun fullscreen() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsets.Type.systemBars())
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    private fun setScreen(screen: View) {
        if (Build.VERSION.SDK_INT < 30) {
            setContentView(screen)
            return
        }
        val host = FrameLayout(this).apply {
            setBackgroundColor(cream)
            addView(screen, FrameLayout.LayoutParams(-1, -1))
            setOnApplyWindowInsetsListener { view, insets ->
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                insets
            }
        }
        setContentView(host)
        host.requestApplyInsets()
    }

    override fun onWindowFocusChanged(focus: Boolean) {
        super.onWindowFocusChanged(focus)
        if (focus) {
            fullscreen()
            checkPin()
        } else if (::board.isInitialized) {
            activeBoard.cancelTouch()
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
        val previousBoard = if (lessonCard != null) lessonBoard else board
        playing = false
        stopLesson()
        syncMusic()
        previousBoard.cancelTouch()
        (previousBoard.parent as? ViewGroup)?.removeView(previousBoard)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = layout()
        val config = resources.configuration
        val inset = (config.screenHeightDp / 36).coerceIn(8,32)
        root.setPadding(dp(12),dp(inset),dp(12),dp(inset))
        val fixedControls = config.screenWidthDp >= 600 && config.screenHeightDp >= 650
        val panel = if (fixedControls) root else menuStack().also { content ->
            root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1,-1))
        }
        setScreen(root)
        panel.item(label(if (session) "Parent menu" else "Little Colors", 34, true), h = 54)
        val sections = controls()
        sections.item(button("Coloring", !learningSelected) { selectSection(false) }.apply { isSelected = !learningSelected },128)
        sections.item(button("Learning", learningSelected) { selectSection(true) }.apply { isSelected = learningSelected },128)
        panel.item(sections,h=-2)
        val content = menuStack()
        if (fixedControls) {
            val scroll = ScrollView(this).apply { addView(content); isFillViewport = true }
            panel.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        } else {
            val rows = if (config.screenWidthDp >= 600) 2 else 4
            content.minimumHeight = dp(62 + rows * (if (rows == 2) 148 else 110))
            panel.addView(content,LinearLayout.LayoutParams(-1,-2))
        }
        if (learningSelected) learningChooser(content) else coloringChooser(content)
        menuModes(panel)
        playSettings(panel)
    }

    private fun menuStack() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        isMotionEventSplittingEnabled = false
    }

    private fun coloringChooser(panel: LinearLayout) {
        val nav = controls()
        nav.item(button("‹") { shelf--; parentScreen() }.apply { isEnabled = shelf > 0; contentDescription = "Previous ten pictures" },52)
        nav.item(label("Levels ${shelf*10+1}–${shelf*10+10}",18,true))
        nav.item(button("›") { shelf++; parentScreen() }.apply { isEnabled = shelf < pictures.lastIndex/10; contentDescription = "Next ten pictures" },52)
        panel.item(nav,h=-2)
        var pages = controls()
        val roomy = resources.configuration.screenWidthDp >= 600
        val columns = if (roomy) 5 else 3
        val tileHeight = if (!roomy) 98 else if (resources.configuration.screenHeightDp >= 850) 136 else 118
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
                        pic.drawIllustration(c, pen)
                        c.restore()
                    }
                }, LinearLayout.LayoutParams(-1, dp(tileHeight - 40)))
                addView(label(pic.name, 14, true))
                setOnClickListener { if (i != board.page) choose(i, board.mode); parentScreen() }
            }
            pages.item(tile, if (roomy) 103 else 84, tileHeight)
        }
    }

    private fun menuModes(panel: LinearLayout) {
        val modes = controls()
        val currentMode = if (learningSelected) learningMode else board.mode
        val width = ((resources.configuration.screenWidthDp - 50) / 3).coerceIn(78,104)
        for ((mode,name) in listOf(Mode.ASSISTED to "Assisted",Mode.FOLLOW to "Follow Along",Mode.FREEHAND to "Freehand")) {
            val option = button(name,currentMode == mode) {
                if (learningSelected) {
                    learningMode = mode
                    preferences.edit().putBoolean("learning.follow",mode == Mode.FOLLOW).apply()
                } else if (mode != board.mode) choose(board.page,mode)
                parentScreen()
            }.apply {
                setPadding(dp(4),0,dp(4),0)
                textSize = 15f
                isSelected = currentMode == mode
                if (learningSelected && mode == Mode.FREEHAND) visibility = View.INVISIBLE
            }
            modes.item(option,width)
        }
        panel.item(modes,h=-2)
    }

    private fun selectSection(learning: Boolean) {
        learningSelected = learning
        preferences.edit().putBoolean("learning.selected", learning).apply()
        parentScreen()
    }

    private fun lastLesson(book: LearningBook): Int {
        val last = if (book == LearningBook.ABC) 25 else 9
        return preferences.getInt("learning.${book.key}.lesson",0).coerceIn(0,last)
    }

    private fun learningChooser(panel: LinearLayout) {
        val books = controls()
        for (book in LearningBook.entries) {
            books.item(button(book.title, learningBook == book) {
                learningBook = book
                learningIndex = lastLesson(book)
                preferences.edit().putString("learning.book", book.key).apply()
                parentScreen()
            }.apply {
                textSize = 24f
                isSelected = learningBook == book
                contentDescription = if (book == LearningBook.ABC) "ABC letters" else "123 numbers"
            },128,60)
        }
        panel.item(books,h=-2)
        val columns = ((resources.configuration.screenWidthDp - 24) / 57).coerceIn(4,10)
        lessons.getValue(learningBook).withIndex().chunked(columns).forEach { group ->
            val row = controls()
            for ((index, item) in group) {
                row.item(button(item.symbol, index == learningIndex) {
                    chooseLesson(index)
                    parentScreen()
                }.apply {
                    isSelected = index == learningIndex
                    textSize = 20f
                    setPadding(0,0,0,0)
                },48,46,3)
            }
            panel.item(row,h=-2)
        }
    }

    private fun chooseLesson(index: Int) {
        learningIndex = index
        preferences.edit().putInt("learning.${learningBook.key}.lesson",index).apply()
    }

    private fun playSettings(panel: LinearLayout) {
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
        val isPinned = pinned
        val pinControl = if (pinAttempt && !isPinned) {
            button("Play is unpinned\nPlay without pinning") { waitingPin = false; play() }.apply {
                contentDescription = "Play without pinning"
                textSize = 14f
            }
        } else {
            label(if (isPinned) "App is pinned" else "Start play to pin the app to the screen.",15)
        }
        panel.item(pinControl,220)
        panel.item(button(if (session && isPinned) "Resume play" else "Start play", true) { requestPin() },220)
        panel.item(button(getString(R.string.privacy_policy_title)) { showPrivacyPolicy() },220,h=44)
        panel.item(label("Vector illustrations adapted from Twemoji · CC BY 4.0",12),h=22)
        panel.item(button("Exit play", action = ::closePlay).apply {
            if (!session) visibility = View.INVISIBLE
        },220)
    }

    private fun showPrivacyPolicy() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.privacy_policy_title)
            .setMessage(R.string.privacy_policy_text)
            .setPositiveButton(android.R.string.ok, null)
        if (!pinned) {
            dialog.setNeutralButton(R.string.privacy_policy_open) { _, _ ->
                val website = Uri.parse(getString(R.string.privacy_policy_url))
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, website))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.privacy_policy_no_browser, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun play() {
        if (learningSelected) {
            playLesson()
            return
        }
        stopNarration()
        session = true; playing = true
        syncMusic()
        root = layout()
        val title = label(pictures[board.page].name, 24, true)
        val pictureNumber = label("★  ${board.page+1} / ${pictures.size}", 13).apply {
            contentDescription = "Picture ${board.page+1} of ${pictures.size}"
        }
        val heading = menuStack().apply {
            gravity = Gravity.CENTER
            addView(title)
            addView(pictureNumber)
        }
        root.addView(playToolbar(heading),LinearLayout.LayoutParams(-1,dp(54)))
        board.background = rounded(paper, 26, 0xffe0d6ea.toInt())
        (board.parent as? ViewGroup)?.removeView(board)
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        footer = controls()
        root.item(footer, w = -1, h = 78)
        showPlaySurface()
        refreshPlay()
    }

    private fun exitButton() = button("×") {}.apply {
        textSize = 26f
        contentDescription = "Hold five seconds to close app"
        bindHold(this,5000L,::closePlay)
    }

    private fun playToolbar(title: View? = null, replay: View? = null) = FrameLayout(this).apply {
        isMotionEventSplittingEnabled = false
        if (title != null) {
            val reserved = dp(if (replay == null) 58 else 118)
            addView(title,FrameLayout.LayoutParams(-1,-1).apply { marginStart = reserved; marginEnd = reserved })
        }
        addView(homeButton(),FrameLayout.LayoutParams(dp(54),dp(48),Gravity.TOP or Gravity.START).apply {
            marginStart = dp(4); topMargin = dp(3)
        })
        addView(exitButton(),FrameLayout.LayoutParams(dp(54),dp(48),Gravity.TOP or Gravity.END).apply {
            marginEnd = dp(4); topMargin = dp(3)
        })
        if (replay != null) {
            addView(replay,FrameLayout.LayoutParams(dp(54),dp(48),Gravity.TOP or Gravity.END).apply {
                marginEnd = dp(62); topMargin = dp(3)
            })
        }
    }

    private fun showPlaySurface() {
        picker = null
        guideTarget = null
        stage = FrameLayout(this).apply { isMotionEventSplittingEnabled = false; addView(root) }
        guide = FingerGuide(this, { playing && guideTarget == null && activeBoard.guidesFollowing }, ::locateFinger)
        stage.addView(guide)
        confetti = Confetti(this)
        stage.addView(confetti)
        if (touching) guide.touch(true)
        setScreen(stage)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fullscreen()
    }

    private fun playLesson() {
        stopLesson()
        session = true
        playing = true
        revealVisible = false
        syncMusic()
        root = layout()

        val replay = button("♫") { speakLesson() }.apply {
            contentDescription = "Replay narration"
            isEnabled = sounds.enabled
        }
        root.addView(playToolbar(label(learningBook.title,25,true),replay),LinearLayout.LayoutParams(-1,dp(54)))

        lessonBoard.openTrace(lesson.strokes,learningMode)
        lessonBoard.contentDescription = lesson.symbol
        (lessonBoard.parent as? ViewGroup)?.removeView(lessonBoard)
        val card = FrameLayout(this).apply {
            isMotionEventSplittingEnabled = false
            background = rounded(paper,26,0xffe0d6ea.toInt())
            cameraDistance = resources.displayMetrics.density * 5500f
            addView(lessonBoard,FrameLayout.LayoutParams(-1,-1))
        }
        lessonCard = card
        root.addView(card,LinearLayout.LayoutParams(-1,0,1f))
        footer = controls()
        root.item(footer,w=-1,h=64)
        showPlaySurface()
        guide.visibility = View.INVISIBLE
        if (resumed) handler.postDelayed(endIntroduction,1200)
        speakLesson()
    }

    private fun revealLesson() {
        val card = lessonCard ?: return
        if (!playing || !learningSelected || revealVisible || !lessonBoard.traceComplete) return
        revealVisible = true
        guide.visibility = View.INVISIBLE
        card.animate().rotationY(90f).setDuration(170).withEndAction {
            if (lessonCard !== card) return@withEndAction
            card.removeAllViews()
            val face = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                contentDescription = "${lesson.revealCaption}. Tap to trace again"
                setOnClickListener { flipBack(card, this) }
                isSoundEffectsEnabled = false
                addView(LessonIllustration(this@PlayActivity, lesson),LinearLayout.LayoutParams(-1,0,1f))
                addView(label(lesson.revealCaption, 34, true).apply {
                    setPadding(dp(12),0,dp(12),dp(16))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },LinearLayout.LayoutParams(-1,-2))
            }
            face.isClickable = false
            card.addView(face,FrameLayout.LayoutParams(-1,-1))
            card.rotationY = -90f
            card.animate().rotationY(0f).setDuration(170).withEndAction {
                if (lessonCard !== card) return@withEndAction
                face.isClickable = true
                val last = learningIndex == lessons.getValue(learningBook).lastIndex
                footer.item(button("➜", true) {
                    if (last) parentScreen()
                    else {
                        chooseLesson(learningIndex+1)
                        playLesson()
                    }
                }.apply {
                    textSize = 26f
                    contentDescription = if (last) "Finish book" else "Next lesson"
                },100,56)
                confetti.burst(false)
                if (resumed) sounds.play(Cue.READY)
                speakLesson()
            }.start()
        }.start()
    }

    private fun flipBack(card: FrameLayout, face: View) {
        face.isClickable = false
        footer.removeAllViews()
        narration.stop()
        card.animate().rotationY(-90f).setDuration(170).withEndAction {
            if (lessonCard !== card) return@withEndAction
            playLesson()
            lessonCard?.apply {
                rotationY = 90f
                animate().rotationY(0f).setDuration(170).start()
            }
        }.start()
    }

    private fun speakLesson() {
        if (resumed && playing && learningSelected && sounds.enabled) {
            narration.play(lesson.recording(revealVisible))
        }
    }

    private fun stopLesson() {
        handler.removeCallbacks(endIntroduction)
        lessonCard?.animate()?.cancel()
        lessonCard = null
        stopNarration()
    }

    private fun stopNarration() {
        handler.removeCallbacks(speakPictureName)
        narration.stop()
    }

    private fun closePlay() {
        waitingPin = false
        activeBoard.cancelTouch()
        stopLesson()
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
                val suggested = color == board.suggestedColor
                val tile = button(if (suggested) "★" else "") { board.pick(color) }.apply {
                    contentDescription = if (suggested) "$name, suggested color" else name
                    textSize = 38f
                    setTextColor(if (Color.luminance(color) < .38f) Color.WHITE else ink)
                    background = rounded(color,24,ink)
                }
                row.addView(tile,LinearLayout.LayoutParams(0,-1,1f).apply { setMargins(dp(5),dp(5),dp(5),dp(5)) })
                if (suggested) guideTarget = tile
            }
            grid.addView(row,LinearLayout.LayoutParams(-1,0,1f))
        }
        overlay.addView(grid)
        overlay.addView(playToolbar(),FrameLayout.LayoutParams(-1,dp(54),Gravity.TOP).apply {
            marginStart = dp(12); marginEnd = dp(12); topMargin = dp(8)
        })
        picker = overlay
        stage.addView(overlay,stage.indexOfChild(guide))
    }

    private fun locateFinger(t: Float, out: PointF): Boolean {
        val drawing = activeBoard
        val target = guideTarget ?: drawing
        if (!target.isShown || target.width <= 0) return false
        target.getLocationInWindow(screenPos)
        stage.getLocationInWindow(stagePos)
        if (target == drawing) drawing.guidePoint(t,out)
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
        if (!playing) parentScreen()
        else if (!learningSelected && board.choosing) refreshPlay()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        syncMusic()
        if (playing && learningSelected && lessonBoard.showingSymbol) handler.postDelayed(endIntroduction,1200)
    }

    override fun onPause() {
        resumed = false
        handler.removeCallbacks(endIntroduction)
        stopNarration()
        syncMusic()
        sounds.stop()
        activeBoard.cancelTouch()
        super.onPause()
    }

    override fun onDestroy() {
        stopLesson()
        music?.release()
        sounds.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Legacy back dispatch is explicitly enabled for pinned play")
    override fun onBackPressed() { if (!playing && !session) super.onBackPressed() }
}
