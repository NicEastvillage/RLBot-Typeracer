package east.rlbot

import east.rlbot.data.BoostPadManager
import east.rlbot.spellinggame.GameLog
import east.rlbot.spellinggame.SpellingGame
import east.rlbot.topology.TriangulationManager

class SpellingBeeBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    var initiated = false
    lateinit var spellingGame: SpellingGame
    lateinit var gameLog: GameLog
    lateinit var tri: TriangulationManager

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        if (!initiated && BoostPadManager.allPads.isNotEmpty()) {
            initiated = true
            spellingGame = SpellingGame(data.allCars.size)
            gameLog = GameLog(spellingGame)
            tri = TriangulationManager(BoostPadManager.allPads.map { it.pos.withZ(25) })
            tri.finish(draw)
        } else if (initiated) {
            //spellingGame.run(data)
            tri.draw(draw)
        }

        return OutputController().withThrottle(1)
    }
}