package east.rlbot

import east.rlbot.data.BoostPad
import east.rlbot.data.BoostPadManager
import east.rlbot.spellinggame.GameLog
import east.rlbot.spellinggame.SpellingGame
import east.rlbot.topology.BoostphobiaGraph
import east.rlbot.topology.astarToPad
import java.awt.Color

class SpellingBeeBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    var initiated = false
    lateinit var spellingGame: SpellingGame
    lateinit var gameLog: GameLog
    lateinit var graph: BoostphobiaGraph
    var lastReselect = 0f
    lateinit var pad: BoostPad

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        if (!initiated && BoostPadManager.allPads.isNotEmpty()) {
            initiated = true
            spellingGame = SpellingGame(data.allCars.size)
            gameLog = GameLog(spellingGame)
            graph = BoostphobiaGraph.load()
            lastReselect = data.match.time
            pad = BoostPadManager.allPads.random()
        } else if (initiated) {
            //spellingGame.run(data)
            if (lastReselect + 2 < data.match.time) {
                //graph = BoostphobiaGraph.load()
                lastReselect = data.match.time
                pad = BoostPadManager.allPads.random()
            }
            graph.render(draw)
            val start = graph.vertices.minByOrNull { it.pos.dist(data.me.pos) }!!
            val path = graph.astarToPad(start, pad)!!
            draw.polyline(listOf(data.me.pos) + path.path.map { it.pos.withZ(20f) }, Color.YELLOW)
        }

        return OutputController().withThrottle(1)
    }
}