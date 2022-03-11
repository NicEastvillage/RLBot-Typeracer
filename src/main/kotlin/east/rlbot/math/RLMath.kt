package east.rlbot.math

import east.rlbot.data.Ball
import east.rlbot.data.Car
import east.rlbot.data.FutureBall
import east.rlbot.util.PIf
import kotlin.math.sqrt


operator fun Number.times(mat: Mat3): Mat3 = mat * this
operator fun Number.times(vec: Vec3): Vec3 = vec * this

fun clamp(value: Double, min: Double, max: Double) = value.coerceIn(min, max)
fun clamp(value: Float, min: Float, max: Float) = value.coerceIn(min, max)

/**
 * Linear interpolation
 */
fun lerp(a: Float, b: Float, t: Float) = (1f - t) * a + t * b

/**
 * Linear interpolation but on angles (between 0 and 2 PI), taking the shortest path.
 */
fun lerpAng(a: Float, b: Float, t: Float): Float {
    // https://gist.github.com/itsmrpeck/be41d72e9d4c72d2236de687f6f53974

    var b = b
    var result: Float
    val diff = b - a
    if (diff < -PIf) {
        // lerp upwards past PI_TIMES_TWO
        b += 2 * PIf
        result = lerp(a, b, t)
        if (result >= 2 * PIf) {
            result -= 2 * PIf
        }
    } else if (diff > PIf) {
        // lerp downwards past 0
        b -= 2 * PIf
        result = lerp(a, b, t)
        if (result < 0f) {
            result += 2 * PIf
        }
    } else {
        // straight lerp
        result = lerp(a, b, t)
    }

    return result
}

/**
 * Inverse linear interpolation
 */
fun invLerp(a: Float, b: Float, v: Float) = (v - a) / (b - a)

/**
 * Returns the intersection point of a 2D circle and a tangent. The circle is located at origin with the given radius
 * and the tangent goes through the given point.
 * When [side] is 1 the right-side tangent is returned, and when [side] is -1 the left-side tangent is returned.
 * `null` is returned when the given point is inside the radius, since no tangents exists in that case.
 */
fun tangentPoint(radius: Float, point: Vec3, side: Float = 1f): Vec3? {
    // https://en.wikipedia.org/wiki/Tangent_lines_to_circles#With_analytic_geometry
    val point2D = point.flat()
    val distSqr = point2D.magSqr()
    if (distSqr < radius * radius) return null // point is inside radius
    val offsetTowardsPoint = point2D * radius * radius / distSqr
    return offsetTowardsPoint - Vec3(-point2D.y, point2D.x) * side * radius * sqrt(distSqr - radius * radius) / distSqr
}

data class HitParameters(
    val impulse: Vec3,
    val hitDir: Vec3,
    val carPos: Vec3,
    // carVel is any velocity where the component in the direction of the normal has a size equal to the impulse
)

/**
 * Approximation figuring out how to change the ball's velocity to the desired velocity.
 * TODO: Numeric method https://discord.com/channels/348658686962696195/535605770436345857/890257276076707950
 */
fun findHitParameters(ball: FutureBall, desiredBallVel: Vec3, carRadius: Float): HitParameters {
    // We assume that the car has a spherical hitbox and that RL has normal physics
    val deltaVel = desiredBallVel - ball.vel
    val normal = deltaVel.dir()
    val impulse = 2 * Ball.MASS * deltaVel / (Ball.MASS + Car.MASS)
    val carPos = ball.pos - (carRadius + Ball.RADIUS) * normal

    // carVel is any velocity where the component in the direction of the normal has a size equal to the impulse
    return HitParameters(
        impulse,
        normal,
        carPos,
    )
}

/**
 * Returns the angle [0-2PI] between two 2D directions. The [sign] parameter should be 1f or -1f and determines
 * if we are going clockwise or counter-clockwise. Similar effect can be achieved by swapping the order of the
 * directions. TODO which sign is clockwise??
 */
fun angleDiff2D(dir1: Vec3, dir2: Vec3, sign: Float = 1f): Float {
    val signedAng = sign * (dir1.atan2() - dir2.atan2())
    return if (signedAng >= 0f) signedAng else signedAng + 2 * PIf
}