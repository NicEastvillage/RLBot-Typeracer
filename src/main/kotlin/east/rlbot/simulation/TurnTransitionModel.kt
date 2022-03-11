package east.rlbot.simulation

object TurnTransitionModel {
    val straightToTurnThrottle = TurnTransitionLUT(
        ClassLoader.getSystemResourceAsStream("AccelerationData/StraightIntoTurnThrottle/StraightToTurnThrottleGraphLUT.csv"),
        (100..2300).step(100).map { ClassLoader.getSystemResourceAsStream("AccelerationData/StraightIntoTurnThrottle/Straight${it}ToTurnThrottle.csv") }
    )
}