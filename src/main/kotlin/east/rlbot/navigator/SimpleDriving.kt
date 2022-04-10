package east.rlbot.navigator

import east.rlbot.BaseBot
import east.rlbot.OutputController
import east.rlbot.data.BoostPadManager
import east.rlbot.data.Car
import east.rlbot.maneuver.SpeedFlip
import east.rlbot.math.Vec3
import east.rlbot.math.tangentPoint
import east.rlbot.simulation.DriveModel
import east.rlbot.simulation.timeSpentTurning
import east.rlbot.simulation.turnRadius
import east.rlbot.util.PIf
import java.awt.Color
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign


class SimpleDriving(val bot: BaseBot) {

    fun towards(
            target: Vec3,
            targetSpeed: Float,
            boostPreservation: Int, // don't use boost if we are below this amount
            allowDodges: Boolean,
    ): OutputController {

        val controls = OutputController()

        val car = bot.data.me

        // If we are on the wall, we choose a target that is closer to the ground.
        // This should make the bot go down the wall.
        val groundTarget = if (car.wheelContact && !car.isUpright)
            car.pos.flat().scaled(0.9)
        else target

        val carToTarget = groundTarget.minus(car.pos)
        val localTarget: Vec3 = car.toLocal(groundTarget)

        val forwardDotTarget = car.ori.forward dot carToTarget.dir()
        val facingTarget = forwardDotTarget > 0.81
        val turnRadius = turnRadius(car.forwardSpeed())
        val inTurnRadius = (localTarget.withZ(0).abs() - Vec3(x = turnRadius + 25)).mag() < turnRadius

        val currentSpeed = car.vel dot carToTarget.dir()
        if (currentSpeed < targetSpeed) {
            // We need to speed up
            if (!inTurnRadius) {
                controls.withThrottle(1.0)
            } else {
                controls.withThrottle(0.05f)
            }
            if (targetSpeed > 1410 && currentSpeed + 60 < targetSpeed && facingTarget && car.boost > boostPreservation) {
                controls.withBoost(true)
            }
            if (allowDodges && currentSpeed + 600 < targetSpeed && car.isUpright && facingTarget && car.timeWithWheelContact > 0.08 && currentSpeed in 1200f..1600f && car.pos.dist(target) > 1750)
                bot.maneuver = SpeedFlip(target, boostPreservation)
        } else {
            // We are going too fast
            val extraSpeed = currentSpeed - targetSpeed
            controls.withThrottle(0.25 - extraSpeed / 500)
        }

        controls.withSteer(localTarget.dir().y * 5)
        if (forwardDotTarget < 0.2 && 1000 < currentSpeed) controls.withThrottle(0f).withSlide()

        return controls
    }

    fun boostPickupTowards(
        target: Vec3,
        targetSpeed: Float,
        boostPreservation: Int // don't use boost if we are below this amount
    ): OutputController {
        val car = bot.data.me
        val carTargetDist = car.pos.dist(target)
        val bestPad = BoostPadManager.allPads.filter { pad ->
            val dist = pad.pos.dist(car.pos)
            (pad.active || dist / pad.timeTillActive > targetSpeed) && pad.pos.dist(car.pos) + pad.pos.dist(target) < 1.3f * carTargetDist
        }.minByOrNull { pad ->
            val score = 1.5f * pad.pos.dist(car.pos) + pad.pos.dist(target) + 0.5f * abs(car.ori.right dot pad.pos)
            // bot.draw.line(pad.pos, pad.pos.withZ(score / 20f), Color.GREEN)
            score
        }
        if (bestPad != null) bot.draw.line(car.pos, bestPad.pos, Color.GREEN)
        return towards(bestPad?.pos ?: target, targetSpeed, boostPreservation, allowDodges = true)
    }

    /**
     * Estimate the minimum time needed to reach the given position from the current position.
     * Returns null if the target position is withing turn radius.
     * TODO: Return endspeed and boost used too
     */
    fun estimateTime2D(pos: Vec3, boostAvailable: Float = bot.data.me.boost.toFloat(), draw: Boolean = false): Float? {

        val car = bot.data.me
        val currentSpeed = car.forwardSpeed()

        // TODO Consider acceleration during turning. This probably has to be found iteratively
        // Turning, assuming constant speed
        val dir = car.pos.dirTo(pos)
        val turnSign = car.ori.toLocal(dir).y.sign
        val radius = turnRadius(currentSpeed)

        val localPosOffset = car.toLocal(pos.flat()) - Vec3(y=turnSign * radius)
        val localTangentPoint = (tangentPoint(radius, localPosOffset, turnSign) ?: return null) + Vec3(y=turnSign * radius)
        // Where we end up after turning
        val tangentPoint = car.toGlobal(localTangentPoint)
        val angle = ((localTangentPoint - Vec3(y=turnSign * radius)).atan2() + turnSign * PIf / 2f).absoluteValue

        if (draw) {
            val mid = car.pos + car.ori.right * turnSign * radius

            bot.draw.color = Color.WHITE
            bot.draw.line(tangentPoint, pos.withZ(Car.REST_HEIGHT))
            if (angle > 0.08) {
                bot.draw.rect3D(tangentPoint, 12, 12)
                bot.draw.string3D(mid.withZ(50), "$angle")
                bot.draw.circle(mid, car.ori.up, radius)

                bot.draw.color = Color.GRAY
                bot.draw.line(car.pos, mid)
                bot.draw.line(mid, tangentPoint)
            }
        }

        val distLeft = tangentPoint.dist2D(pos)
        val timeSpent = timeSpentTurning(currentSpeed, angle)

        return timeSpent + DriveModel.drive1D(distLeft, currentSpeed, boostAvailable).timeSpent
    }
}