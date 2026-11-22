package family.littlecolors

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

internal enum class Cue(val sample: Int) {
    TOUCH(R.raw.touch), SWATCH(R.raw.swatch), READY(R.raw.ready), FINISH(R.raw.finish)
}

internal class PlaySounds(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = mutableSetOf<Int>()
    private var pencilStream = 0
    private var pencilWanted = false
    private var pencilVolume = 0f
    private var pencilFade: ValueAnimator? = null
    private var stream = 0
    private var lastTouch = -1000L
    var enabled = true
        set(value) { field = value; if (!value) stop() }
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) { loaded.add(id); if (pencilWanted) pencil(true) } }
    }
    private val ids = Cue.values().associateWith { pool.load(context, it.sample, 1) }

    private val scratch = pool.load(context, R.raw.pencil, 1)

    fun pencil(active: Boolean) {
        val wanted = active && enabled
        val changed = wanted != pencilWanted
        pencilWanted = wanted
        if (wanted && pencilStream == 0) {
            if (scratch !in loaded) return
            pencilStream = pool.play(scratch, 0f, 0f, 1, -1, 1f)
            if (pencilStream != 0) fadePencil(.32f, 85)
        } else if (changed && pencilStream != 0) {
            fadePencil(if (wanted) .32f else 0f, if (wanted) 85 else 115)
        }
    }

    private fun fadePencil(target: Float, milliseconds: Long) {
        cancelPencilFade()
        pencilFade = ValueAnimator.ofFloat(pencilVolume, target).apply {
            duration = milliseconds
            addUpdateListener {
                pencilVolume = it.animatedValue as Float
                pool.setVolume(pencilStream, pencilVolume, pencilVolume)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (pencilFade !== animation) return
                    pencilFade = null
                    if (target == 0f) stopPencil()
                }
            })
        }
        pencilFade?.start()
    }

    private fun cancelPencilFade() {
        val fade = pencilFade
        pencilFade = null
        fade?.cancel()
    }

    private fun stopPencil() {
        cancelPencilFade()
        if (pencilStream != 0) pool.stop(pencilStream)
        pencilStream = 0
        pencilVolume = 0f
    }

    fun play(cue: Cue) {
        val id = ids.getValue(cue)
        if (!enabled || id !in loaded) return
        val now = SystemClock.uptimeMillis()
        if (cue == Cue.TOUCH && now - lastTouch < 450) return
        if (cue == Cue.TOUCH) lastTouch = now
        pool.stop(stream)
        stream = pool.play(id, .55f, .55f, 1, 0, 1f)
    }

    fun stop() {
        pencilWanted = false
        stopPencil()
        pool.stop(stream)
    }

    fun release() {
        stop()
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}
