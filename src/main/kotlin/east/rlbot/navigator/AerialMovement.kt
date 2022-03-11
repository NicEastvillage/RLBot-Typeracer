package east.rlbot.navigator

import east.rlbot.BaseBot
import east.rlbot.OutputController
import east.rlbot.math.Mat3
import kotlin.math.atan2
import kotlin.math.pow

class AerialMovement(val bot: BaseBot) {

    /**
     * Returns the controls in order to align the cars orientation with the given target.
     * When [upIsImportant] is false, the alignment will prioritise find the forward direction before the up,
     * other up will be prioritised before forward. In other words, set [upIsImportant] to false during aerials,
     * and set [upIsImportant] to true when we want to land on our wheels.
     */
    fun align(
        targetOri: Mat3,
        upIsImportant: Boolean = false,
    ): OutputController {

        val controls = OutputController()

        val localForward = bot.data.me.ori.toLocal(targetOri.forward)
        val localUp = bot.data.me.ori.toLocal(targetOri.up)
        val localAngVel = bot.data.me.ori.toLocal(bot.data.me.angVel)

        val pitchAng = atan2(-localForward.z, localForward.x)
        val pitchAngVel = localAngVel.y

        val yawAng = atan2(-localForward.y, localForward.x)
        val yawAngVel = -localAngVel.z

        val rollAng = atan2(-localUp.y, localUp.z)
        val rollAngVel = localAngVel.x

        val rollScale =
            if (upIsImportant) 1f
            else (targetOri.forward dot bot.data.me.ori.forward).let { if (it > 0.5f) it.pow(1.5f) else 0f }

        val yawScale =
            if (upIsImportant) (targetOri.up dot bot.data.me.ori.up).let { if (it > 0.5f) it.pow(1.5f) else 0f }
            else 1f

        controls
            .withRoll((-3.3f * rollAng + 0.5f * rollAngVel) * rollScale)
            .withPitch(-3.8f * pitchAng + 0.8f * pitchAngVel)
            .withYaw((-3.8f * yawAng + 0.9f * yawAngVel) * yawScale)
            .withThrottle(1f)

        return controls
    }
}