package east.rlbot.data

import east.rlbot.math.AimCone
import east.rlbot.math.Vec3
import east.rlbot.math.lerp
import east.rlbot.simulation.BallPredictionManager
import rlbot.flat.PredictionSlice

data class FutureBall(
    val pos: Vec3,
    val vel: Vec3,
    val time: Float,
) {
    constructor(predictionSlice: PredictionSlice) : this(
        Vec3(predictionSlice.physics().location()),
        Vec3(predictionSlice.physics().velocity()),
        predictionSlice.gameSeconds(),
    )

    fun valid(): Boolean {
        return BallPredictionManager.getAtTime(time)?.let { (it.pos - pos).magSqr() < 100f } ?: false
    }

    fun adjustable() = AdjustableFutureBall(this)
}

class AdjustableFutureBall(
    ball: FutureBall
) {
    val original = ball
    var ball: FutureBall = ball; private set
    val pos get() = ball.pos
    val vel get() = ball.vel
    val time get() = ball.time

    var valid = true; private set

    /**
     * Check if prediction changed. If the change is very small, update ball. If change is big, become invalid.
     * If a better time is given (usually expected arrival), the time will be moved slightly towards the better time.
     */
    fun adjust(betterTime: Float = time, allowErrorMargin: Float = 15f) {
        val newTime = lerp(time, betterTime, 0.05f)
        val alternatives = BallPredictionManager.getBundleAtTime(newTime)
        val closest = alternatives.minByOrNull { it.pos.distSqr(pos) }
        if (closest == null ||
            closest.pos.distSqr(pos) > allowErrorMargin * allowErrorMargin ||
            closest.pos.distSqr(original.pos) > allowErrorMargin * allowErrorMargin * 1.5f
        ) {
            valid = false
        } else {
            ball = closest
        }
    }
}

class AdjustableAimedFutureBall(
    ball: FutureBall,
    private val aimFactory: (FutureBall) -> AimCone,
) {
    val ball = AdjustableFutureBall(ball)
    val pos get() = ball.pos
    val vel get() = ball.vel
    val time get() = ball.time
    val valid get() = ball.valid

    var aimCone = aimFactory(ball); private set

    /**
     * Check if prediction changed. If the change is very small, update ball. If change is big, become invalid.
     * Then update the aim cone.
     * If a better time is given (usually expected arrival), the time will be moved slightly towards the better time.
     */
    fun adjust(betterTime: Float = time, allowErrorMargin: Float = 15f) {
        ball.adjust(betterTime, allowErrorMargin)
        aimCone = aimFactory(ball.ball)
    }
}