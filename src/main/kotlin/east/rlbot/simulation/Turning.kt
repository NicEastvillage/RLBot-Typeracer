package east.rlbot.simulation

fun turnRadius(forwardSpeed: Float): Float {
    if (forwardSpeed == 0f) return 0f
    return 1.0f / turnCurvature(forwardSpeed)
}

fun turnCurvature(forwardSpeed: Float): Float {
    if (0f <= forwardSpeed && forwardSpeed < 500f)
        return 0.006900f - 5.84e-6f * forwardSpeed
    if (500f <= forwardSpeed && forwardSpeed < 1000f)
        return 0.005610f - 3.26e-6f * forwardSpeed
    if (1000f <= forwardSpeed && forwardSpeed < 1500f)
        return 0.004300f - 1.95e-6f * forwardSpeed
    if (1500.0 <= forwardSpeed && forwardSpeed < 1750f)
        return 0.003025f - 1.1e-6f * forwardSpeed
    if (1750f <= forwardSpeed && forwardSpeed < 2500f)
        return 0.001800f - 4e-7f * forwardSpeed
    return 0f
}

/**
 * Returns the number of seconds spent turning the given angle assuming a constant forward velocity
 */
fun timeSpentTurning(forwardVel: Float, angle: Float): Float {
    return angle / (forwardVel * turnCurvature(forwardVel))
}