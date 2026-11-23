package family.littlecolors

import android.content.Context
import android.graphics.Path
import java.util.Locale
import org.json.JSONObject

internal enum class LearningBook(val key: String, val title: String) {
    ABC("abc", "ABC"), NUMBERS("numbers", "123")
}

internal data class LearningLesson(
    val book: LearningBook,
    val symbol: String,
    val caption: String,
    val illustration: Picture,
    val count: Int,
    val strokes: List<Path>,
) {
    val revealCaption = if (book == LearningBook.ABC) "$symbol ${symbol.lowercase(Locale.ROOT)} · $caption" else caption
    fun recording(reveal: Boolean) =
        "learning/speech/${book.key}_${symbol.lowercase(Locale.ROOT)}_${if (reveal) "reveal" else "intro"}.wav"
}

internal fun loadLearningBooks(context: Context): Map<LearningBook, List<LearningLesson>> {
    fun read(file: String) = JSONObject(context.assets.open("learning/$file.json").bufferedReader().use { it.readText() })

    val artwork = read("illustrations")
    val illustrations = pictures.associateBy { it.name }.toMutableMap()
    for (name in artwork.keys()) {
        val layers = readPaths(artwork.getJSONArray(name))
        illustrations[name] = Picture(name, layers.map { it.path }, swatches = emptyList(),
            previewColors = layers.map { it.color })
    }

    val catalog = read("lessons")
    return LearningBook.entries.associateWith { book ->
        val lessons = catalog.getJSONArray(book.key)
        List(lessons.length()) { i ->
            val row = lessons.getJSONArray(i)
            val symbol = row.getString(0)
            val guides = row.getJSONArray(3)
            LearningLesson(book, symbol, row.getString(1), illustrations.getValue(row.getString(2)),
                if (book == LearningBook.ABC) 1 else symbol.toInt(),
                List(guides.length()) { vectorPath(guides.getString(it)) })
        }
    }
}
