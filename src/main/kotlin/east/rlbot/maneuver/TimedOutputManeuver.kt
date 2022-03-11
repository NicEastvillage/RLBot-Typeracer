package east.rlbot.maneuver

import east.rlbot.OutputController
import east.rlbot.data.DataPack

open class TimedOutputManeuver(private val duration: Float, private val action: (DataPack) -> OutputController?) : Maneuver {

    private var begin: Float? = null
    private var end: Float = 0f

    override var done: Boolean = false

    override fun exec(data: DataPack): OutputController? {
        if (begin == null) {
            // Lazily set begin so we can use it in SteppedManeuver
            begin = data.match.time
            end = begin!! + duration
        }
        done = end <= data.match.time
        return action(data)
    }
}