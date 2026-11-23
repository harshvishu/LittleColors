package family.littlecolors

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

internal class LessonNarration(
    private val context: Context,
    private val onPlayingChanged: (Boolean) -> Unit,
) {
    private var player: MediaPlayer? = null

    fun play(recording: String) {
        stop()
        val next = MediaPlayer()
        player = next
        try {
            next.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            context.assets.openFd(recording).use {
                next.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            next.setOnCompletionListener { if (player === it) stop() }
            next.setOnErrorListener { failed, _, _ ->
                if (player === failed) stop()
                true
            }
            next.prepare()
            next.start()
            onPlayingChanged(true)
        } catch (error: IOException) {
            Log.w("LessonNarration", "Could not play $recording", error)
            stop()
        }
    }

    fun stop() {
        val previous = player ?: return
        player = null
        previous.release()
        onPlayingChanged(false)
    }
}
