package east.rlbot.maneuver

import east.rlbot.OutputController
import east.rlbot.data.DataPack

class ConditionalSingleOutputManeuver(condition: (DataPack) -> Boolean, output: OutputController) : ConditionalOutputManeuver(condition, { output })