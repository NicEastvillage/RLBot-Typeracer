package east.rlbot

import east.rlbot.data.BoostPadManager
import east.rlbot.spellinggame.GameLog
import east.rlbot.spellinggame.SpellingGame

class SpellingBeeBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    var initiated = false
    lateinit var spellingGame: SpellingGame
    lateinit var gameLog: GameLog

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        if (!initiated && BoostPadManager.allPads.isNotEmpty()) {
            initiated = true
            spellingGame = SpellingGame(data.allCars.size)
            gameLog = GameLog(spellingGame)
        } else if (initiated) {
            spellingGame.run(data)
        }

        return OutputController().withThrottle(1)
    }
}