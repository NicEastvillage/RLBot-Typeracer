package east.rlbot.maneuver

import east.rlbot.OutputController
import east.rlbot.data.DataPack
import east.rlbot.util.DebugDraw

open class ConditionalOutputManeuver(private val condition: (DataPack) -> Boolean, private val action: (DataPack) -> OutputController?) : Maneuver {

    override var done: Boolean = false

    override fun exec(data: DataPack): OutputController? {
        if (condition(data))
            return action(data)
        else
            done = true
        return null
    }
}