package east.rlbot

import east.rlbot.data.BoostPadManager
import east.rlbot.topology.BoostphobiaGraph
import east.rlbot.topology.astarToPad
import east.rlbot.typinggame.ALL_WORDS_SHUFFLED
import east.rlbot.typinggame.GameLog
import east.rlbot.typinggame.TypeRacerGame

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
            //graph.render(draw)

            val gameMe = typeRacerGame.players[index]
            val targetWords = gameMe.objectives.map { ALL_WORDS_SHUFFLED[it] }
            val targetWord = targetWords.firstOrNull { it.startsWith(gameMe.inputBuffer) }
            if (targetWord == null) {
                val pad = BoostPadManager.bigPads.minByOrNull { it.pos.dist(data.me.pos) }
                    ?: return OutputController().withThrottle(data.me.forwardSpeed() / -1000f)
                return drive.towards(pad.pos, 600 + data.me.pos.dist(pad.pos), 0, false)
            }

            val nextLetter = targetWord[gameMe.inputBuffer.length]
            val pad = typeRacerGame.padLetters.filter { it.value == nextLetter }.minByOrNull { it.key.pos.dist(data.me.pos) }?.key
                ?: return OutputController().withThrottle(data.me.forwardSpeed() / -1000f)

            val start = graph.vertices.filter { it.pad == null }.minByOrNull { it.pos.dist(data.me.pos) }!!
            val path = graph.astarToPad(start, pad)!!
            //draw.polyline(listOf(data.me.pos) + path.path.map { it.pos.withZ(20f) }, Color.YELLOW)

            val targetVertPos = if (path.path[0].pos.dist(pad.pos) < data.me.pos.dist(pad.pos)) path.path[0].pos else path.path[1].pos
            val nextVertPos = (if (path.path.size <= 2) null else path.path[2].pos)
                ?: return drive.towards(targetVertPos, 700f, 100, false)

            val offsetDir = targetVertPos.dirTo(data.me.pos.lerp(nextVertPos, 0.5f))
            val offsetTarget = targetVertPos + offsetDir * 250f
            val arriveDir = data.me.pos.dirTo(nextVertPos)
            val finalTarget = offsetTarget - arriveDir * data.me.pos.dist(offsetTarget) / 2f

            return drive.towards(finalTarget, 1000f, 100, false)
        }

        return OutputController().withThrottle(1)
    }
}