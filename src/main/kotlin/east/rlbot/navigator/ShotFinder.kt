package east.rlbot.navigator

import east.rlbot.BaseBot
import east.rlbot.data.AdjustableAimedFutureBall
import east.rlbot.data.Arena
import east.rlbot.maneuver.Maneuver
import east.rlbot.maneuver.strike.*
import east.rlbot.math.AimCone
import east.rlbot.math.Vec3
import east.rlbot.simulation.BallPredictionManager
import east.rlbot.simulation.SLICES_PR_SEC

class ShotFinder(val bot: BaseBot) {

    fun shootAt(
        target: Vec3
    ): Maneuver? {
        // TODO
        return null
    }

    fun findSoonestStrike(
        timeLimit: Float = 4f, strikeFactories: List<StrikeFactory> = listOf(
            DodgeStrike,
            ChipStrike,
            //CatchIntoDribble,
            AerialStrike,
        )
    ): Strike? {
        val slices = (SLICES_PR_SEC * timeLimit).toInt().coerceAtLeast(0)
        return BallPredictionManager.latest?.subList(0, slices)?.mapNotNull { ball ->
            if (ball.pos.y > Arena.LENGTH2 + 5f) return@findSoonestStrike null // Goal scored, abandon search
            strikeFactories.mapNotNull { factory ->
                factory.tryCreate(bot.data, AdjustableAimedFutureBall(ball) {
                    AimCone.atGoal(it.pos, bot.data.enemyGoal)
                })
            }.firstOrNull()
        }?.firstOrNull()
    }
}