package east.rlbot.simulation

import east.rlbot.math.Vec3
import kotlin.math.pow

data class RigidBody(
    val pos: Vec3,
    val vel: Vec3,
)

object Physics {
    val GRAVITY = Vec3(z=-650f)

    fun fall(rb: RigidBody, time: Float) = RigidBody(
        GRAVITY * time.pow(2) * 0.5f + rb.vel * time + rb.pos,
        GRAVITY * time + rb.vel,
    )
}