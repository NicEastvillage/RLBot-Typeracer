package east.rlbot.maneuver.strike

import east.rlbot.OutputController
import east.rlbot.data.AdjustableAimedFutureBall
import east.rlbot.data.Ball
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.maneuver.DodgeFinish
import east.rlbot.math.Mat3
import east.rlbot.math.Vec3
import east.rlbot.simulation.JumpModel
import east.rlbot.util.DT
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

class DodgeStrikeDodge(
    aimedBall: AdjustableAimedFutureBall,
) : Strike(aimedBall) {

    private val FIRST_JUMP_PAUSE_DURATION = 2 * DT

    override var done = false

    private var expectedFirstJumpDuration = JumpModel.single.simUntilLimit(heightLimit = aimedBall.pos.z).time

    private var initialized = false
    private var jumping = true
    private var startTime = 0f
    private var jumpEndTime = 0f

    override fun exec(data: DataPack): OutputController {
        if (!initialized) {
            initialized = true
            startTime = data.match.time
        }
        val car = data.me
        aimedBall.adjust()
        val height = aimedBall.pos.z

        val jumpTimeLeft = (startTime + expectedFirstJumpDuration - data.match.time).coerceAtLeast(0f)

        // Do we need to boost while jumping?
        val boostDuration = min(car.boost / Car.BOOST_USAGE_RATE, jumpTimeLeft) + FIRST_JUMP_PAUSE_DURATION
        val xyDisplacementDuringJumpWithoutBoost = car.vel.flat() * (jumpTimeLeft + FIRST_JUMP_PAUSE_DURATION)
        val xyDisplacementDuringJumpWithBoost = car.ori.forward.flat() * Car.BOOST_BONUS_ACC * boostDuration.pow(2) / 2 + xyDisplacementDuringJumpWithoutBoost
        val expectedDodgePos = (car.pos + xyDisplacementDuringJumpWithoutBoost).withZ(height)
        val boost = expectedDodgePos.dist(aimedBall.pos) > Ball.RADIUS + car.hitbox.size.x / 2f + 10f

        val ori = Mat3.lookingAt(car.pos, aimedBall.pos)
        val controls = data.bot.fly.align(ori).withJump(jumping).withBoost(boost)

        // Do phase change?
        if (jumping && jumpTimeLeft <= 0f) {
            jumping = false
            jumpEndTime = data.match.time
        } else if (!jumping && jumpEndTime + FIRST_JUMP_PAUSE_DURATION <= data.match.time) {
            data.bot.maneuver = DodgeFinish(aimedBall.pos)
        }

        if (car.wheelContact && data.match.time - startTime > 0.03f) {
            // Something went wrong. We jumped, but now we have wheel contact
            done = true
        }

        return controls
    }

    fun canBegin(data: DataPack): Boolean {
        val car = data.me
        val height = aimedBall.pos.z
        val result = JumpModel.single.simUntilLimit(heightLimit = height)
        val timeLeft = aimedBall.time - data.match.time
        val boostTime = min(car.boost / Car.BOOST_USAGE_RATE, result.time)
        val xyDisplacementDuringJumpWithoutBoost = car.vel.flat() * (result.time + 0.02f)
        val xyDisplacementDuringJumpWithBoost = car.ori.forward.flat() * Car.BOOST_BONUS_ACC * boostTime.pow(2) / 2 + xyDisplacementDuringJumpWithoutBoost
        val expectedDodgePosWithoutBoost = car.pos + Vec3(z=height)

        // Test multiple dodge positions
        var expectedDodgePosIsGood = false
        val N = 5
        for (i in 0..N) {
            val expectedDodgePosF = expectedDodgePosWithoutBoost + xyDisplacementDuringJumpWithBoost * (i / N.toFloat())
            expectedDodgePosIsGood = expectedDodgePosIsGood || expectedDodgePosF.dist(aimedBall.pos) < Ball.RADIUS + car.hitbox.size.x / 2f + 10f
        }

        // Since we are jumping immediately, difference between timeLeft and jump time should be very small
        val timeIsGood = abs(timeLeft - result.time) < 0.1f
        return expectedDodgePosIsGood && timeIsGood
    }
}