package family.littlecolors

import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun oval(x: Int, y: Int, rx: Int, ry: Int = rx) = Path().apply {
    addOval((x-rx).toFloat(), (y-ry).toFloat(), (x+rx).toFloat(), (y+ry).toFloat(), Path.Direction.CW)
}
private fun box(l: Int, t: Int, r: Int, b: Int, radius: Int = 8) = Path().apply {
    addRoundRect(l.toFloat(),t.toFloat(),r.toFloat(),b.toFloat(),radius.toFloat(),radius.toFloat(),Path.Direction.CW)
}
private fun polygon(vararg xy: Int) = Path().apply {
    moveTo(xy[0].toFloat(),xy[1].toFloat())
    for (i in 2 until xy.size step 2) lineTo(xy[i].toFloat(),xy[i+1].toFloat())
    close()
}
private fun star(x: Int, y: Int, outer: Int, inner: Int, tips: Int = 5): Path {
    val points = IntArray(tips*4)
    for (i in 0 until tips*2) {
        val angle = i*PI/tips - PI/2
        val r = if (i%2 == 0) outer else inner
        points[2*i] = (x + r*cos(angle)).toInt()
        points[2*i+1] = (y + r*sin(angle)).toInt()
    }
    return polygon(*points)
}
private fun page(name: String, vararg parts: Path) = Picture(name, parts.toList())

internal val extraPictures = listOf(
    page("Moon", Path().apply { moveTo(195f,45f); cubicTo(35f,36f,38f,261f,225f,235f); cubicTo(113f,191f,121f,103f,195f,45f); close() }),
    page("Sunshine", star(160,148,105,79,12),oval(160,148,60)),
    page("Kite", polygon(160,40,245,127,160,239,75,127),polygon(144,246,159,253,177,245,177,267,160,258,144,267)),
    page("Little House",box(82,141,238,246),polygon(55,143,160,48,265,143),box(143,178,178,246)),
    vectorPage("Pear"),
    page("Water Drop",Path().apply { moveTo(160f,38f); cubicTo(148f,97f,76f,143f,98f,204f); cubicTo(139f,298f,295f,219f,215f,121f); close() }),
    page("Shooting Star",polygon(44,220,143,112,199,139,99,218,126,175),star(197,108,68,31)),
    vectorPage("Carrot"),
    vectorPage("Peas"),
    vectorPage("Cherries"),
    vectorPage("Pineapple"),
    vectorPage("Strawberry"),
    vectorPage("Orange Slice"),
    vectorPage("Radish"),
    vectorPage("Mushroom"),
    page("Ice Pop",box(148,187,173,258,12),box(101,53,219,206,38)),
    vectorPage("Ice Cream"),
    vectorPage("Cupcake"),
    vectorPage("Pizza Slice"),
    page("Cookie",oval(160,150,93),oval(121,113,14),oval(190,125,16),oval(148,190,18)),
    page("Birthday Cake",box(67,139,253,235),box(94,109,226,145),box(151,70,169,109),oval(160,52,10,17)),
    page("Tea Cup",oval(227,144,40,30),oval(228,144,22,15),box(73,100,217,201,30),oval(151,223,97,12)),
    page("Lemonade",polygon(95,97,224,97,202,245,118,245),polygon(160,99,177,48,213,36,218,48,187,59,175,99),oval(110,98,23)),
    page("Jam Jar",box(94,84,226,244,23),box(88,63,232,91),box(107,134,213,193,14)),
    page("Picnic Basket",oval(160,125,78,67),oval(160,123,59,47),polygon(56,144,265,144,233,236,89,236)),
    page("Frying Pan",box(170,115,278,137),oval(120,168,67),oval(120,168,48)),
    page("Fork",polygon(105,47,124,47,124,111,149,111,149,46,170,46,170,111,195,111,195,47,214,47,214,120,174,152,172,249,148,249,146,153,105,122)),
    page("Teapot",polygon(113,146,44,109,55,158,103,188),oval(227,146,33,37),oval(229,146,19,20),oval(159,166,68,54),box(119,93,199,111)),
    vectorPage("Fish"),
    vectorPage("Whale"),
    page("Jellyfish",box(102,139,123,227,10),box(150,139,170,246,10),box(197,139,218,226,10),Path().apply {moveTo(66f,154f);cubicTo(64f,43f,252f,43f,254f,154f);close()}),
    vectorPage("Turtle"),
    page("Crab",oval(160,168,67,43),polygon(102,161,54,126,52,92,68,111,89,87,87,123,113,146),polygon(216,160,263,124,268,93,248,111,234,90,233,124,207,145),oval(137,130,11),oval(182,130,11)),
    page("Seahorse",polygon(181,64,217,80,242,108,211,121,185,113,195,146,184,176,142,202,146,224,171,223,183,206,185,235,159,250,122,237,109,201,140,154,129,120,145,85),oval(187,90,7)),
    page("Starfish",star(160,150,102,40),oval(150,140,7),oval(171,140,7)),
    vectorPage("Snail"),
    vectorPage("Butterfly"),
    vectorPage("Ladybird"),
    vectorPage("Bee"),
    page("Dragonfly",oval(104,113,49,20),oval(216,113,49,20),oval(107,159,46,17),oval(213,159,46,17),oval(160,163,12,76),oval(160,84,19)),
    page("Caterpillar",oval(81,179,29),oval(127,169,33),oval(174,156,34),oval(224,138,36),oval(239,128,7)),
    page("Rabbit",oval(126,94,20,57),oval(186,88,21,56),oval(157,177,68,62),oval(157,186,13,8)),
    page("Bear",oval(106,99,27),oval(214,99,27),oval(160,159,75),oval(160,181,34,24),oval(160,171,10,7)),
    page("Fox",polygon(77,87,127,109,191,109,242,86,224,166,161,226,95,169),polygon(141,194,181,194,161,217)),
    page("Cat",polygon(86,167,86,68,130,99,184,99,228,67,236,164,220,201,160,223,101,204),oval(132,153,9),oval(189,153,9),polygon(149,173,172,173,160,184)),
    page("Dog",oval(160,159,63,68),oval(97,135,21,56),oval(221,135,21,56),oval(160,180,21,13)),
    page("Mouse",oval(103,107,40),oval(217,107,40),oval(160,166,65,60),oval(160,187,12,9)),
    page("Owl",polygon(95,64,126,92,192,92,227,64,227,173,208,219,161,239,112,218,95,179),oval(133,135,24),oval(185,135,24),polygon(147,159,173,159,160,179)),
    vectorPage("Duck"),
    vectorPage("Frog"),
    vectorPage("Elephant"),
    page("Lion",star(160,152,101,78,12),oval(160,153,58),polygon(147,162,173,162,160,175)),
    page("Penguin Chick",oval(95,166,23,42),oval(225,166,23,42),oval(160,151,67,91),oval(160,175,42,53),polygon(146,123,176,123,160,136)),
    page("Pine Tree",box(142,190,178,246),polygon(160,38,226,126,201,126,249,197,72,197,119,126,93,126)),
    page("Oak Tree",box(142,140,180,252),oval(160,113,89,66)),
    vectorPage("Tulip"),
    page("Daisy",box(154,142,166,250),oval(160,81,25,40),oval(118,121,39,25),oval(202,121,39,25),oval(160,161,25,37),oval(160,120,24)),
    vectorPage("Cactus"),
    page("Acorn",oval(160,168,56,68),box(89,95,231,137,19),polygon(149,96,158,59,181,53,173,96)),
    page("Leaf",polygon(68,201,85,141,132,92,187,71,249,62,235,131,201,182,145,208,92,212),polygon(71,225,62,217,198,112,204,114)),
    page("Mountain",polygon(45,223,156,56,272,223),polygon(118,114,156,56,195,112,169,101,151,122,139,103)),
    page("Island",oval(158,222,103,23),polygon(131,209,153,91,168,91,160,213),star(164,93,62,15,6)),
    page("Snowman",oval(160,198,62,51),oval(160,116,46),box(125,54,195,80),box(110,75,211,87),polygon(161,119,196,126,161,132)),
    page("Cloud",Path().apply {moveTo(87f,196f);cubicTo(34f,187f,53f,116f,101f,124f);cubicTo(96f,48f,204f,44f,213f,116f);cubicTo(275f,103f,286f,195f,235f,196f);close()}),
    page("Thunderstorm",oval(154,105,99,42),polygon(154,149,202,149,173,184,198,184,125,258,147,201,120,201)),
    page("Snowflake",star(160,150,103,32,6),star(160,150,49,23,6)),
    page("Volcano",polygon(53,234,126,95,190,95,270,234),polygon(111,124,126,95,190,95,218,145,188,130,178,155,152,119,134,139),oval(157,69,15,12)),
    page("Planet",oval(160,148,65),polygon(48,178,110,142,248,109,272,114,203,151,73,187)),
    page("Rocket",polygon(116,180,80,211,92,152,125,129),polygon(200,181,239,211,229,152,197,129),polygon(119,180,119,108,159,42,201,109,201,180),oval(160,119,21),polygon(139,188,182,188,161,251)),
    page("Robot",box(94,74,226,154,15),box(109,162,211,230),oval(130,112,12),oval(190,112,12),box(145,45,175,70)),
    page("Train",box(72,118,230,190),box(83,74,139,128),box(184,84,211,119),oval(107,202,23),oval(198,202,23)),
    page("Bus",box(44,91,276,201,19),box(63,109,112,148),box(130,109,182,148),box(199,109,251,148),oval(95,206,22),oval(224,206,22)),
    page("Truck",box(44,86,174,187),polygon(174,122,220,122,256,155,267,187,174,187),oval(95,197,24),oval(222,197,24)),
    page("Airplane",polygon(37,152,126,132,167,63,193,63,174,132,246,132,267,109,277,109,264,163,175,170,189,223,167,226,126,174,51,170)),
    page("Helicopter",oval(141,166,65,40),polygon(190,153,261,112,269,123,203,172),box(151,97,164,129),box(64,85,247,98),box(105,211,206,223)),
    page("Submarine",oval(157,163,93,54),box(145,74,164,112),box(148,72,188,85),oval(122,158,19),oval(188,158,19)),
    page("Hot Air Balloon",oval(160,112,77,70),polygon(107,163,213,163,180,206,140,206),box(135,213,185,245)),
    page("Tractor",box(78,142,239,188),box(98,85,161,145),oval(110,189,39),oval(219,193,25)),
    page("Lighthouse",polygon(101,247,129,99,192,99,219,247),box(119,74,201,106),polygon(110,74,160,40,210,74),box(144,147,174,177)),
    page("Tent",polygon(44,225,153,55,273,225),polygon(116,225,155,149,201,225)),
    page("Castle",box(79,120,242,235),box(65,90,109,235),box(213,90,256,235),polygon(55,90,86,46,119,90),polygon(201,90,234,46,267,90),box(141,177,182,235,18)),
    page("Windmill",polygon(104,245,132,118,185,118,217,245),star(160,115,84,19,4),oval(160,115,14)),
    page("Umbrella",Path().apply {moveTo(46f,146f);cubicTo(69f,39f,247f,39f,274f,146f);close()},box(153,148,167,231),box(130,221,167,238)),
    page("Rain Boot",polygon(103,67,187,67,183,176,239,187,253,210,244,230,92,230),box(98,58,192,82)),
    page("Mitten",box(106,65,201,209,43),oval(202,145,26,37),box(108,204,193,240)),
    page("Beanie",Path().apply {moveTo(69f,195f);cubicTo(73f,43f,239f,43f,251f,195f);close()},box(64,186,257,219),oval(160,63,19)),
    page("Bow Tie",polygon(58,89,157,134,157,164,58,211),polygon(263,89,164,134,164,164,263,211),box(141,126,179,175)),
    page("Backpack",oval(160,83,34,31),box(85,85,235,239,30),box(110,158,209,219,15)),
    page("Drum",box(73,102,246,211,14),oval(160,105,87,23),polygon(88,139,126,192,163,139,199,193,229,137,199,203,162,151,126,203)),
    vectorPage("Guitar"),
    page("Bell",polygon(75,198,105,163,114,108,138,80,178,80,204,108,214,164,245,198),oval(160,211,21,14),oval(159,64,12)),
    page("Toy Blocks",box(61,151,152,235),box(158,151,249,235),box(109,62,199,145)),
    page("Camera",box(56,97,265,215,18),box(99,74,158,100),oval(165,155,45),oval(165,155,26),box(222,111,244,125)),
    page("Clock",oval(160,151,94),oval(160,151,74),polygon(151,91,165,91,165,146,196,164,190,176,151,155)),
    page("Treasure Chest",box(64,124,257,226,14),box(64,83,257,145,23),box(145,131,175,170)),
    page("Crown",polygon(57,87,113,132,160,58,207,132,263,87,235,216,85,216),oval(160,163,16),box(85,213,235,230)),
    page("Celebration Cake",box(62,168,258,237),box(91,119,229,168),box(118,88,202,120),star(160,62,23,11))
)
