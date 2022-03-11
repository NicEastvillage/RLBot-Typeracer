package east.rlbot

import east.rlbot.data.BoostPadManager
import east.rlbot.spellinggame.SpellingGame

class SpellingBeeBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    var initiated = false
    lateinit var spellingGame: SpellingGame

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        if (!initiated && BoostPadManager.allPads.isNotEmpty()) {
            initiated = true
            spellingGame = SpellingGame(data.allCars.size)
        } else if (initiated) {
            spellingGame.run(data)
        }

        return OutputController().withThrottle(1)
    }
}