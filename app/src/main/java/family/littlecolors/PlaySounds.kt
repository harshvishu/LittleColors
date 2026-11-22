package family.littlecolors

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
        pencilWanted = active && enabled
        if (!pencilWanted) {
            if (pencilStream != 0) pool.stop(pencilStream)
            pencilStream = 0
        } else if (scratch in loaded && pencilStream == 0) {
            pencilStream = pool.play(scratch,.40f,.40f,1,-1,1f)
        }
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

    fun stop() { pencil(false); pool.stop(stream) }
    fun release() { pool.release() }
}
