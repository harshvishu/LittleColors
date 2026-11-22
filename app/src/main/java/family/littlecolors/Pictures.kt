package family.littlecolors

import android.graphics.Path

internal data class Picture(val name: String, val regions: List<Path>, val details: List<Path> = emptyList(),
    val previewColors: List<Int> = emptyList(), val marks: List<ColoredPath> = emptyList()) {
    val outlines = regions + details
}

