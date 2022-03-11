package east.rlbot.simulation

import east.rlbot.data.FutureBall
import rlbot.cppinterop.RLBotDll
import rlbot.cppinterop.RLBotInterfaceException

const val PREDICTION_LENGTH = 6
const val SLICES_PR_SEC = 60
const val NR_OF_SLICES = PREDICTION_LENGTH * SLICES_PR_SEC

object BallPredictionManager {

    private var latestTime: Float? = null
    var latest: List<FutureBall>? = null; private set

    fun update(time: Float) {
        synchronized(this) {
            val latestTime = latestTime
            if (latestTime != null && time <= latestTime) return
            try {
                val ballPrediction = RLBotDll.getBallPrediction()
                latest = (0 until NR_OF_SLICES).map { FutureBall(ballPrediction.slices(it)) }
                this.latestTime = time
            } catch (ignored: RLBotInterfaceException) {
            } catch (ignored: IndexOutOfBoundsException) {
            }
        }
    }

    fun getAtTime(time: Float): FutureBall? {
        val lt = latestTime ?: return null
        val i = ((time - lt) * SLICES_PR_SEC).toInt()
        return if (i in 0 until NR_OF_SLICES) latest?.get(i) else null
    }

    fun getBundleAtTime(time: Float): List<FutureBall> {
        val bundle = mutableListOf<FutureBall>()
        val lt = latestTime ?: return bundle
        val i = ((time - lt) * SLICES_PR_SEC).toInt()
        if (i - 1 in 0 until NR_OF_SLICES) latest!![i - 1].let { bundle.add(it) }
        if (i in 0 until NR_OF_SLICES) latest!![i].let { bundle.add(it) }
        if (i + 1 in 0 until NR_OF_SLICES) latest!![i + 1].let { bundle.add(it) }
        return bundle
    }
}