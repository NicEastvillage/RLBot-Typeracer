package east.rlbot.data

import east.rlbot.math.Vec3

class BoostPad(
    val index: Int,
    val pos: Vec3,
    val isBig: Boolean,
) {
    var active = true
    var timeTillActive = 0f
    val refreshTime = if (isBig) 10f else 4f
    val amount = if (isBig) 100 else 12
}