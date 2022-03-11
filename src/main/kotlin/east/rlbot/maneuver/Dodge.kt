package east.rlbot.maneuver

import east.rlbot.OutputController
import east.rlbot.math.Vec3
import kotlin.math.sign

class Dodge(
    target: Vec3?,
    firstJumpDuration: Float = 0.15f,
    firstPauseDuration: Float = 0.02f,
) : SteppedManeuver(
    TimedSingleOutputManeuver(firstJumpDuration, OutputController().withThrottle(1).withJump()),
    TimedSingleOutputManeuver(firstPauseDuration, OutputController().withThrottle(1)),
    DodgeFinish(target)
)

class DodgeFinish(
    target: Vec3?,
    secondJumpDuration: Float = 0.08f,
    secondPauseDuration: Float = 0.42f,
) : SteppedManeuver(
    TimedOutputManeuver(secondJumpDuration) { data ->
        if (target == null) {
            OutputController().withThrottle(1f).withJump().withPitch(-1f)
        } else {
            val dir = data.me.toLocal(target).flat().dir()
            OutputController()
                .withThrottle(1f)
                .withJump()
                .withPitch(-dir.x)
                .withYaw(data.me.ori.up.z.sign * dir.y)
        }
    },
    TimedSingleOutputManeuver(secondPauseDuration, OutputController().withThrottle(1)),
    Recovery()
)