package east.rlbot.maneuver

import east.rlbot.OutputController

class TimedSingleOutputManeuver(duration: Float, output: OutputController) : TimedOutputManeuver(duration, { output })