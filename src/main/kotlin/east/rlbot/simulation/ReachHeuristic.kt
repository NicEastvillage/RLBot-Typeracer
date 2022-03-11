package east.rlbot.simulation

import east.rlbot.data.DataPack
import east.rlbot.data.Car
import east.rlbot.math.Vec3
import rlbot.cppinterop.RLBotDll
import rlbot.cppinterop.RLBotInterfaceException


data class ReachPrediction(val where: Vec3, val wen: Float)

/**
 * Returns the time and position of the until the earliest moment where the given car can reach
 * the ball under certain assumptions.
 */
fun reachHeuristic(
        data: DataPack,
        car: Car
) : ReachPrediction? {

    try {
        val ballPrediction = RLBotDll.getBallPrediction()

        var i = 0
        while (i < NR_OF_SLICES) {

            val slice = ballPrediction.slices(i)
            val position = Vec3(slice.physics().location())
            val time = i / 60f
            if (position.z > 100) {
                i += 6
                continue
            }
            val carToBall = position.minus(car.pos)
            val carToBallDir = carToBall.dir()
            val dist = carToBall.mag().toDouble()
            val speedTowardsBall = car.vel dot carToBallDir
            val averageSpeed = (speedTowardsBall + Car.MAX_SPEED) / 2.0f
            val travelTime = dist / averageSpeed
            if (travelTime < time) {
                return ReachPrediction(position, time)
            }

            i += 6
        }
    } catch (ignored: RLBotInterfaceException) {
    } catch (ignored: IndexOutOfBoundsException) {
    }

    return null
}