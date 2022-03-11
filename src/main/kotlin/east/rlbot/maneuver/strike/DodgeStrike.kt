package east.rlbot.maneuver.strike

import east.rlbot.OutputController
import east.rlbot.data.AdjustableAimedFutureBall
import east.rlbot.data.Ball
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.maneuver.Recovery
import east.rlbot.simulation.JumpModel
import java.awt.Color
import kotlin.math.max

class DodgeStrike(
    aimedBall: AdjustableAimedFutureBall,
) : Strike(aimedBall) {

    override var done: Boolean = false

    private val dodgeStrikeDodge = DodgeStrikeDodge(aimedBall)

    override fun exec(data: DataPack): OutputController {
        val car = data.me

        val betterStrike = data.bot.shotFinder.findSoonestStrike(aimedBall.time - data.match.time)
        if (betterStrike != null) {
            data.bot.maneuver = betterStrike
        }

        if (!car.wheelContact)
            data.bot.maneuver = Recovery()

        // Find positions and directions
        val desiredBallVel = aimedBall.aimCone.centerDir * max(aimedBall.vel.mag(), 750f)
        val arriveDir = (desiredBallVel - aimedBall.vel).dir()
        val arrivePos = (aimedBall.pos - arriveDir * (Ball.RADIUS + car.hitbox.size.x / 2f)).withZ(Car.REST_HEIGHT)

        // Find speed
        val timeLeft = aimedBall.time - data.match.time
        val speed = car.pos.dist(arrivePos) / timeLeft

        // Adjust ball using estimated time
        val time = data.bot.drive.estimateTime2D(arrivePos)
        if (time != null)
            aimedBall.adjust(betterTime = time)

        // Start dodge?
        if (dodgeStrikeDodge.canBegin(data))
            data.bot.maneuver = dodgeStrikeDodge

        done = timeLeft <= 0 || speed > Car.MAX_SPEED + 10f || (speed > Car.MAX_THROTTLE_SPEED + 10f && car.boost == 0) || !aimedBall.valid

        data.bot.draw.crossAngled(aimedBall.pos, 85f, Color.MAGENTA)
        data.bot.draw.line(car.pos, arrivePos, Color.CYAN)

        return data.bot.drive.towards(arrivePos, speed, 0, allowDodges = false)
    }

    companion object Factory : StrikeFactory {
        override fun tryCreate(data: DataPack, aimedBall: AdjustableAimedFutureBall): Strike? {
            val car = data.me
            if (Ball.RADIUS * 1.25f > aimedBall.pos.z && aimedBall.pos.y * data.bot.team.ysign > 0f) return null // If rolling, then only on opponent half
            if (JumpModel.single.maxHeight() + Ball.RADIUS / 5f < aimedBall.pos.z) return null
            if (aimedBall.vel.flat().mag() > 230f && aimedBall.vel.angle2D(car.vel) < 1.3f) return null

            val desiredBallVel = aimedBall.aimCone.centerDir * max(aimedBall.vel.mag(), 300f)
            val arriveDir = (desiredBallVel - aimedBall.vel).dir()
            val arrivePos = (aimedBall.pos - arriveDir * (Ball.RADIUS + car.hitbox.size.x / 2f)).withZ(Car.REST_HEIGHT)

            if ((data.bot.drive.estimateTime2D(arrivePos) ?: Float.MAX_VALUE) > aimedBall.time - data.match.time) return null
            return DodgeStrike(aimedBall)
        }
    }
}