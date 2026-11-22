package family.littlecolors

import android.graphics.Path

internal data class Picture(val name: String, val regions: List<Path>, val details: List<Path> = emptyList(),
    val previewColors: List<Int> = emptyList(), val marks: List<ColoredPath> = emptyList()) {
    val outlines = regions + details
}

internal val pictures = listOf(
    Picture("Balloon", listOf(Path().apply {
        moveTo(160f, 216f)
        cubicTo(112f, 198f, 71f, 145f, 77f, 98f)
        cubicTo(85f, 16f, 228f, 15f, 242f, 99f)
        cubicTo(249f, 145f, 213f, 196f, 160f, 216f)
        close()
    }), listOf(Path().apply {
        moveTo(160f, 216f)
        cubicTo(193f, 242f, 130f, 250f, 153f, 280f)
    })),
    Picture("Apple", listOf(Path().apply {
        moveTo(159f, 101f)
        cubicTo(76f, 54f, 42f, 143f, 86f, 222f)
        cubicTo(114f, 279f, 137f, 245f, 159f, 250f)
        cubicTo(197f, 264f, 218f, 260f, 242f, 202f)
        cubicTo(275f, 113f, 229f, 63f, 159f, 101f)
        close()
    }, Path().apply {
        moveTo(166f, 76f)
        quadTo(181f, 28f, 226f, 31f)
        quadTo(222f, 74f, 166f, 76f)
        close()
    }), listOf(Path().apply {
        moveTo(159f, 101f)
        quadTo(168f, 65f, 150f, 40f)
    })),
    Picture("Sailboat", listOf(Path().apply {
        moveTo(48f, 202f); lineTo(272f, 202f); lineTo(232f, 247f); lineTo(91f, 247f); close()
    }, Path().apply {
        moveTo(152f, 51f); lineTo(152f, 188f); lineTo(61f, 188f); close()
    }, Path().apply {
        moveTo(165f, 70f); quadTo(213f, 110f, 244f, 188f); lineTo(165f, 188f); close()
    }), listOf(Path().apply {
        moveTo(158f, 44f); lineTo(158f, 202f)
    }))
) + extraPictures
