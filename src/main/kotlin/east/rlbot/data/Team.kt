package east.rlbot.data

import java.awt.Color

enum class Team(
    val index: Int,
    val ysign: Int,
    val color: Color,
    val altColor: Color,
    val altAltColor: Color,
) {
    BLUE(0, -1, Color.BLUE, Color(150, 0, 255), Color.GREEN),
    ORANGE(1, 1, Color.ORANGE, Color.RED, Color.YELLOW);

    companion object {
        fun get(index: Int) = if (index == 0) BLUE else ORANGE
    }

    fun other(): Team = if (index == 0) ORANGE else BLUE
}
