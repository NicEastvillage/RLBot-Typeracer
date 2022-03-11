package east.rlbot.simulation

object AccelerationModel {
    val throttle = StraightAccelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/StraightThrottle.csv"))
    val boost = StraightAccelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/StraightBoost.csv"))

    val turnThrottle = TurningAccelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/TurningThrottle.csv"))
    val turnBoost = TurningAccelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/TurningBoost.csv"))

    // Assumes start acceleration is above 1300 uu/s
    val turnThrottleDecel = TurningDecelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/TurningThrottleDecel.csv"))
    val turnCoastingDecel = TurningDecelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/TurningCoastingDecel.csv"))
    val turnBrakingDecel = TurningDecelerationLUT(ClassLoader.getSystemResourceAsStream("AccelerationData/TurningBrakingDecel.csv"))
}
