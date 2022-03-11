package east.rlbot.maneuver.strike

import east.rlbot.OutputController
import east.rlbot.data.AdjustableAimedFutureBall
import east.rlbot.data.Ball
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.maneuver.Recovery
import java.awt.Color
import kotlin.math.max

class CatchIntoDribble(
    aimedBall: AdjustableAimedFutureBall,
) : Strike(aimedBall) {

    override var done: Boolean = false; private set

    override fun exec(data: DataPack): OutputController {

        val betterStrike = data.bot.shotFinder.findSoonestStrike(aimedBall.time - data.match.time, listOf(CatchIntoDribble))
        if (betterStrike != null) {
            data.bot.maneuver = betterStrike
        }

        if (!data.me.wheelContact)
            data.bot.maneuver = Recovery()

        aimedBall.adjust()

        val desiredBallVel = aimedBall.aimCone.centerDir * max(aimedBall.vel.mag(), 300f)

        val arriveDir = (desiredBallVel - aimedBall.vel).dir()
        val arrivePos = (aimedBall.pos - arriveDir * (Ball.RADIUS + data.me.hitbox.size.x / 2f)).withZ(Car.REST_HEIGHT)
        val timeLeft = aimedBall.time - data.match.time
        val aveSpeed = data.me.pos.dist(arrivePos) / timeLeft
        val speed = (aveSpeed - 100f) * 1.1f // TODO

        done = timeLeft <= 0 || aveSpeed > Car.MAX_SPEED + 10f || (aveSpeed > Car.MAX_THROTTLE_SPEED + 10f && data.me.boost == 0) || !aimedBall.valid

        data.bot.draw.crossAngled(aimedBall.pos, 85f, Color.YELLOW)
        data.bot.draw.line(data.me.pos, arrivePos, Color.CYAN)

        return data.bot.drive.towards(arrivePos, speed, 0, allowDodges = false)
    }

    companion object Factory : StrikeFactory {
        override fun tryCreate(data: DataPack, aimedBall: AdjustableAimedFutureBall): Strike? {
            val car = data.me
            if (Ball.RADIUS + Car.REST_HEIGHT < aimedBall.pos.z) return null

            val desiredBallVel = aimedBall.aimCone.centerDir * max(aimedBall.vel.mag(), 300f)
            val arriveDir = (desiredBallVel - aimedBall.vel).dir()
            val arrivePos = (aimedBall.pos - arriveDir * (Ball.RADIUS + car.hitbox.size.x / 2f)).withZ(Car.REST_HEIGHT)

            if ((data.bot.drive.estimateTime2D(arrivePos) ?: Float.MAX_VALUE) > aimedBall.time - data.match.time) return null

            return CatchIntoDribble(aimedBall)
        }
    }
}