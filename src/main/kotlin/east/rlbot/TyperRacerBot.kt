package east.rlbot

import east.rlbot.data.BoostPadManager
import east.rlbot.topology.BoostphobiaGraph
import east.rlbot.topology.astarToPad
import east.rlbot.typinggame.ALL_WORDS_SHUFFLED
import east.rlbot.typinggame.GameLog
import east.rlbot.typinggame.TypeRacerGame
import java.awt.Color
import kotlin.math.pow

class TyperRacerBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    var initiated = false
    lateinit var typeRacerGame: TypeRacerGame
    lateinit var gameLog: GameLog
    lateinit var graph: BoostphobiaGraph

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        if (!initiated && BoostPadManager.allPads.isNotEmpty()) {
            initiated = true
            typeRacerGame = TypeRacerGame(data.allCars.size)
            gameLog = GameLog(typeRacerGame)
            graph = BoostphobiaGraph.load()
        } else if (initiated) {

            typeRacerGame.run(data)
            graph.render(draw)

            val gameMe = typeRacerGame.players[index]
            val targetWords = gameMe.objectives.map { ALL_WORDS_SHUFFLED[it] }
            val targetWord = targetWords.firstOrNull { it.startsWith(gameMe.inputBuffer) }
            if (targetWord == null) {
                val pad = BoostPadManager.bigPads.filter { it.active }.minByOrNull { it.pos.dist(data.me.pos) }
                    ?: return OutputController().withThrottle(data.me.forwardSpeed() / -1000f)
                return drive.towards(pad.pos, 600 + data.me.pos.dist(pad.pos), 0, false)
            }

            val nextLetter = targetWord[gameMe.inputBuffer.length]
            val pad = typeRacerGame.padLetters.filter { it.value == nextLetter }.minByOrNull { it.key.pos.dist(data.me.pos) }?.key
                ?: return OutputController().withThrottle(data.me.forwardSpeed() / -1000f)

            val start = graph.vertices.filter { it.pad == null }.minByOrNull { it.pos.dist(data.me.pos + data.me.vel * 0.05f) }!!
            val path = graph.astarToPad(start, pad)!!.let {
                if (it[0].pos.dist(pad.pos) < data.me.pos.dist(pad.pos)) it else it.drop(1)
            }
            draw.polyline(listOf(data.me.pos) + path.map { it.pos.withZ(20f) }, Color.YELLOW)

            if (path.size <= 1) return drive.towards(path[0].pos, 1150f, 100, false)

            val targetVertPos = path[0].pos
            val nextVertPos = path[1].pos


            val t = 1f / ((data.me.pos.dist(targetVertPos) / 400f) + 1)
            val finalTarget = targetVertPos.lerp(nextVertPos, t.pow(1.6f))

            draw.line(data.me.pos, finalTarget.withZ(20), Color.WHITE)

            val straightness = data.me.pos.dirTo(finalTarget).dot(finalTarget.dirTo(nextVertPos))

            return drive.towards(finalTarget, 1300f + straightness * 400f, 0, false)
        }

        return OutputController().withThrottle(1)
    }
}