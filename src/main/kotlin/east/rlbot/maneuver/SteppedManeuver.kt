package east.rlbot.maneuver

import east.rlbot.OutputController
import east.rlbot.data.DataPack

open class SteppedManeuver(val sequence: List<Maneuver>) : Maneuver {

    constructor(vararg steps: Maneuver) : this(steps.toList())

    private var currentIndex = 0

    override val done: Boolean get() = currentIndex >= sequence.size

    override fun exec(data: DataPack): OutputController? {
        while (!done) {
            val current = sequence[currentIndex]
            if (!current.done) {
                current.exec(data)?.let { return it }
            }
            currentIndex++
        }
        return null
    }
}