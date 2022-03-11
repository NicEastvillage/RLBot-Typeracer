package east.rlbot.maneuver.strike

import east.rlbot.OutputController
import east.rlbot.data.AdjustableAimedFutureBall
import east.rlbot.data.DataPack
import east.rlbot.maneuver.Maneuver

abstract class Strike(
    var aimedBall: AdjustableAimedFutureBall,
) : Maneuver {
    abstract override fun exec(data: DataPack): OutputController
}

interface StrikeFactory {
    fun tryCreate(data: DataPack, aimedBall: AdjustableAimedFutureBall): Strike?
}