package east.rlbot

import east.rlbot.data.BoostPadManager
import east.rlbot.spellinggame.GameLog
import east.rlbot.spellinggame.SpellingGame
import east.rlbot.topology.BoostphobiaGraph

class SpellingBeeBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    var initiated = false
    lateinit var spellingGame: SpellingGame
    lateinit var gameLog: GameLog
    lateinit var graph: BoostphobiaGraph
    var lastLoad = 0f

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        if (!initiated && BoostPadManager.allPads.isNotEmpty()) {
            initiated = true
            spellingGame = SpellingGame(data.allCars.size)
            gameLog = GameLog(spellingGame)
            graph = BoostphobiaGraph.load()
            lastLoad = data.match.time
        } else if (initiated) {
            //spellingGame.run(data)
            if (lastLoad + 1 < data.match.time) {
                graph = BoostphobiaGraph.load()
                lastLoad = data.match.time
            }
            graph.render(draw)
        }

        return OutputController().withThrottle(1)
    }
}