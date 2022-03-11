package east.rlbot.data

import east.rlbot.math.Vec3
import rlbot.flat.GameInfo

class Match {
    var dt = 0.1666f
    var time = 0f
    var timeRemaining = 99999f
    var overTime = false
    var roundActive = false
    var matchEnded = false
    var isTimerPausedDueToKickOff = false
    var isKickOff = false
    var isFirstFrameOfKickOff = false

    fun update(info: GameInfo, ball: Ball) {
        dt = info.secondsElapsed() - time
        time = info.secondsElapsed()
        timeRemaining = info.gameTimeRemaining()
        overTime = info.isOvertime()
        roundActive = info.isRoundActive()
        matchEnded = info.isMatchEnded()
        isTimerPausedDueToKickOff = info.isKickoffPause()

        val wasKickoff = isKickOff
        isKickOff = ball.pos.flat() == Vec3.ZERO && isTimerPausedDueToKickOff
        isFirstFrameOfKickOff = !wasKickoff && isKickOff
    }
}