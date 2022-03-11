package east.rlbot.maneuver.strike

import east.rlbot.OutputController
import east.rlbot.data.AdjustableAimedFutureBall
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.maneuver.Recovery
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max

class ChipStrike(
    var car: Car,
    interceptBall: AdjustableAimedFutureBall,
) : Strike(interceptBall) {

    override var done: Boolean = false

    override fun exec(data: DataPack): OutputController {

        val betterStrike = data.bot.shotFinder.findSoonestStrike(aimedBall.time - data.match.time)
        if (betterStrike != null) {
            data.bot.maneuver = betterStrike
        }

        if (!data.me.wheelContact)
            data.bot.maneuver = Recovery()

        // Find positions and directions
        val desiredBallVel = aimedBall.aimCone.centerDir * max(aimedBall.vel.mag(), 750f)
        val arriveDir = (desiredBallVel - aimedBall.vel).dir()
        val arrivePos = (aimedBall.pos - arriveDir * 100).withZ(Car.REST_HEIGHT)

        // Find speed
        val timeLeft = aimedBall.time - data.match.time
        val speed = data.me.pos.dist(arrivePos) / timeLeft

        // Adjust ball using estimated time
        val time = data.bot.drive.estimateTime2D(arrivePos)
        if (time != null)
            aimedBall.adjust(betterTime = time)

        done = timeLeft <= 0 || speed > Car.MAX_SPEED + 10f || (speed > Car.MAX_THROTTLE_SPEED + 10f && data.me.boost == 0) || !aimedBall.valid

        data.bot.draw.crossAngled(aimedBall.pos, 85f, Color.GREEN)
        data.bot.draw.line(data.me.pos, arrivePos, Color.CYAN)

        return data.bot.drive.towards(arrivePos, speed, 0, allowDodges = false)
    }

    companion object Factory : StrikeFactory {
        override fun tryCreate(data: DataPack, aimedBall: AdjustableAimedFutureBall): Strike? {
            val car = data.me
            if (aimedBall.pos.z > 190 - abs(aimedBall.vel.z) / 5f || abs(aimedBall.vel.z) > 280) return null
            if (aimedBall.vel.flat().mag() > 200f && aimedBall.vel.angle2D(car.vel) < 1.3f) return null

            val desiredBallVel = aimedBall.aimCone.centerDir * max(aimedBall.vel.mag(), 300f)
            val arriveDir = (desiredBallVel - aimedBall.vel).dir()
            val arrivePos = (aimedBall.pos - arriveDir * 100).withZ(Car.REST_HEIGHT)

            if ((data.bot.drive.estimateTime2D(arrivePos) ?: Float.MAX_VALUE) > aimedBall.time - data.match.time) return null

            return ChipStrike(car, aimedBall)
        }
    }
}
