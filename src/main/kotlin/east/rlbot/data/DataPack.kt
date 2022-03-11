package east.rlbot.data

import east.rlbot.BaseBot
import east.rlbot.simulation.BallPredictionManager
import rlbot.flat.GameTickPacket

class DataPack(val bot: BaseBot, val index: Int) {

    val match = Match()

    val me = Car(bot.index, bot.team, bot.name)

    val allCars = mutableListOf<Car>()
    val wholeTeam = mutableListOf<Car>() // includes us!
    val allies = mutableListOf<Car>()
    val enemies = mutableListOf<Car>()

    val ball = Ball()

    val myGoal = Goal.get(bot.team)
    val enemyGoal = Goal.get(bot.team.other())

    fun update(packet: GameTickPacket) {

        ball.update(packet.ball(), packet.gameInfo().secondsElapsed())
        match.update(packet.gameInfo(), ball)

        BallPredictionManager.update(match.time)
        BoostPadManager.update(packet)

        // Update cars
        for (carIndex in 0 until packet.playersLength()) {
            val carInfo = packet.players(carIndex)

            if (carIndex >= allCars.size) {
                // We found a new car
                val newCar = if (carIndex == index) me else Car(carIndex, Team.get(carInfo.team()), carInfo.name())

                // Add new car to relevant lists
                allCars += newCar
                if (newCar.team == bot.team) {
                    wholeTeam += newCar
                    if (carIndex != index) {
                        allies += newCar
                    }
                } else {
                    enemies += newCar
                }
            }

            allCars[carIndex].update(carInfo)
        }
    }
}