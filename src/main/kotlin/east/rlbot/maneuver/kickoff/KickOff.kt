package east.rlbot.maneuver.kickoff

import east.rlbot.BaseBot
import east.rlbot.OutputController
import east.rlbot.data.Ball
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.maneuver.*
import east.rlbot.math.Vec3
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

enum class KickOffType {
    Corner, BackSide, FarBack
}

fun decideKickoff(bot: BaseBot) {
    assert(bot.data.ball.pos == Vec3.ZERO) { "Ball is not a (0, 0)" }

    val teamByDist = bot.data.wholeTeam.sortedBy { it.pos.mag() + it.pos.x.sign * bot.team.ysign }
    if (teamByDist.indexOf(bot.data.me) == 0) {
        val type = when (abs(bot.data.me.pos.y)) {
            in 0f..3500f -> KickOffType.Corner
            in 3500f..4000f -> KickOffType.BackSide
            else -> KickOffType.FarBack
        }
        bot.maneuver = SpeedFlipKickOff(type, false)
    }
    else {
        bot.maneuver = TimedOutputManeuver(1f) { OutputController().withThrottle(0f) }
    }
}

class SimpleKickOff() : SteppedManeuver(
    ConditionalOutputManeuver({ it.bot.data.me.pos.dist(it.bot.data.ball.pos) > 720 }, {
        val dist = it.bot.data.me.pos.dist(it.bot.data.ball.pos)
        it.bot.drive.towards(Vec3(y=it.bot.team.ysign * (dist * 0.5f - 500f + Random.nextFloat() * 10f)), Car.MAX_SPEED, 0, allowDodges = false)
    }),
    Dodge(Vec3(z=Ball.RADIUS))
)

class GeneralKickOff() : Maneuver {
    override var done = false

    private var dodge: Dodge? = null

    override fun exec(data: DataPack): OutputController? {
        done = !data.ball.pos.flat().isZero
        if (done) {
            // Promote dodge maneuver
            data.bot.maneuver = dodge
            return dodge?.exec(data)
        }
        if (dodge?.done == true) {
            dodge = null
        }
        if (dodge != null) {
            return dodge!!.exec(data)
        }

        val car = data.me
        val dist = car.pos.mag()

        if (dist <= 720)
            dodge = Dodge(Vec3(z=Ball.RADIUS))

        var target = Vec3(Random.nextFloat() * 10f, car.team.ysign * (dist * 0.5f - 500f))

        if (abs(car.pos.x) > 230 && abs(car.pos.y) > 2880) {
            target = target.withY(car.team.ysign * 2770)
        }

        return data.bot.drive.towards(target, Car.MAX_SPEED, 0, allowDodges = false)
    }
}

class SpeedFlipKickOff(
    val type: KickOffType,
    val chipAroundEnemy: Boolean, // Only works against "slow" bots
) : Maneuver {
    override var done = false

    private var speedFlip: SpeedFlip? = null
    private var hasSpeedFlipped = false

    override fun exec(data: DataPack): OutputController? {
        done = !data.ball.pos.flat().isZero
        if (done) {
            return speedFlip?.exec(data)
        }
        if (speedFlip?.done == true) {
            speedFlip = null
            hasSpeedFlipped = true
        }
        if (speedFlip != null) {
            return speedFlip!!.exec(data)!!.withBoost(data.me.vel.mag() < 2250)
        }

        val car = data.me
        val dist = car.pos.mag()

        // Decide when to do speed flip
        val flipSpeed = when (type) {
            KickOffType.Corner -> 650
            KickOffType.BackSide -> 1190
            else -> 950
        }
        if (car.vel.mag() > flipSpeed && !hasSpeedFlipped) {
            speedFlip = SpeedFlip(Vec3(y=car.team.ysign * 100), 100)
        }

        // Dodge when close
        if ((type == KickOffType.FarBack || !chipAroundEnemy) && dist < 760)
            data.bot.maneuver = Dodge(Vec3(0f, car.team.ysign * 125, Ball.RADIUS))

        val target = if (dist < 650)
            // Turn to the side to hit with corner of car. This can chip it around slow bots
            Vec3(x=car.pos.x.sign * 100)
        else if (type == KickOffType.Corner)
            Vec3(y=car.team.ysign * 100)
        else if (type == KickOffType.BackSide && abs(car.pos.x) > 50 && abs(car.pos.y) > 3420)
            Vec3(y=car.team.ysign * 2830)
        else
            Vec3(Random.nextFloat() * 10f, car.team.ysign * (dist * 0.5f - 500f))

        return data.bot.drive.towards(target, Car.MAX_SPEED, 0, allowDodges = false)
    }
}