package east.rlbot.util

import java.awt.Color

fun Boolean.toInt() = if (this) 1 else 0

fun Float.coerceIn01() = this.coerceIn(0f, 1f)

const val PIf = Math.PI.toFloat()

fun Color.half() = Color(red / 2, green / 2, blue / 2)