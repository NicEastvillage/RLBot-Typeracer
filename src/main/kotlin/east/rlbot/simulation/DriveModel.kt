package east.rlbot.simulation

import east.rlbot.data.Car

object DriveModel {

    data class Result1D(
        val timeSpent: Float,
        val endSpeed: Float,
        val boostUsed: Float,
    )

    /**
     * Estimate the minimum time needed to drive a straight distance
     */
    fun drive1D(dist: Float, currentSpeed: Float, boostAvailable: Float): Result1D {
        var currentSpeed = currentSpeed
        var boostLeft = boostAvailable
        var distLeft = dist
        var timeSpent = 0f

        var accelerationResult: StraightAccelerationLUT.LookupResult? = null

        // Accelerate with boost
        if (boostAvailable > 0) {
            val boostTime = boostAvailable / Car.BOOST_USAGE_RATE
            accelerationResult = AccelerationModel.boost.simUntilLimit(currentSpeed, distanceLimit = distLeft, timeLimit = boostTime)
            distLeft -= accelerationResult.distance
            timeSpent += accelerationResult.duration
            currentSpeed = accelerationResult.endSpeed
            boostLeft -= accelerationResult.duration * Car.BOOST_USAGE_RATE
        }

        // Accelerate with throttle
        if (distLeft > 0f && currentSpeed <= Car.MAX_THROTTLE_SPEED) {
            accelerationResult = AccelerationModel.throttle.simUntilLimit(currentSpeed, distanceLimit = distLeft)
            distLeft -= accelerationResult.distance
            timeSpent += accelerationResult.duration
            currentSpeed = accelerationResult.endSpeed
        }

        // If distance was not reached during acceleration, travel remain distance with constant speed
        if (accelerationResult == null || accelerationResult.distanceLimitReached)
            timeSpent += distLeft / currentSpeed

        return Result1D(
            timeSpent = timeSpent,
            endSpeed = currentSpeed,
            boostUsed = boostAvailable - boostLeft,
        )
    }
}