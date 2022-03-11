package east.rlbot.data

import east.rlbot.math.Vec3
import rlbot.cppinterop.RLBotDll
import rlbot.cppinterop.RLBotInterfaceException
import rlbot.flat.GameTickPacket

object BoostPadManager {

    private var latestTime = -1f

    var allPads = listOf<BoostPad>(); private set
    var bigPads = listOf<BoostPad>(); private set
    var smallPads = listOf<BoostPad>(); private set

    fun update(packet: GameTickPacket) {
        synchronized(this) {
            val time = packet.gameInfo().secondsElapsed()
            if (time <= latestTime) return // Already updated this frame
            try {
                // Initialize pads
                if (packet.boostPadStatesLength() != allPads.size) {
                    initialize()
                }
                // Update pad state
                for (i in 0 until packet.boostPadStatesLength()) {
                    val padState = packet.boostPadStates(i)
                    val pad = allPads[i]
                    pad.active = padState.isActive
                    pad.timeTillActive = pad.refreshTime - padState.timer()
                }
            } catch (e: RLBotInterfaceException) {}
        }
    }

    /**
     * Initialize boost pads.
     * May throw RLBotInterfaceException.
     */
    private fun initialize() {
        val fieldInfo = RLBotDll.getFieldInfo()
        allPads = (0 until fieldInfo.boostPadsLength()).map { i ->
            val pad = fieldInfo.boostPads(i)
            BoostPad(i, Vec3(pad.location()), pad.isFullBoost)
        }
        bigPads = allPads.filter { it.isBig }
        smallPads = allPads.filter { !it.isBig }
    }
}