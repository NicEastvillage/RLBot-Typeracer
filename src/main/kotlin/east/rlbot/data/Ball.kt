package east.rlbot.data

import east.rlbot.math.Vec3
import rlbot.flat.BallInfo

class Ball {
    lateinit var pos: Vec3
    lateinit var vel: Vec3
    var time: Float = 0f

    fun update(info: BallInfo, time: Float) {
        val phy = info.physics()
        pos = Vec3(phy.location())
        vel = Vec3(phy.velocity())
        this.time = time
    }

    fun asFuture() = FutureBall(pos, vel, time)

    companion object {
        const val MASS = 30f
        const val RADIUS = 92.1f
    }
}