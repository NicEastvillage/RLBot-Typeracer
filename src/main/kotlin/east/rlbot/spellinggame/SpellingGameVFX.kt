package east.rlbot.spellinggame

import east.rlbot.data.BoostPadManager
import east.rlbot.data.DataPack
import east.rlbot.data.Goal
import east.rlbot.math.Vec3
import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.BallState
import rlbot.gamestate.GameState
import rlbot.gamestate.PhysicsState
import java.awt.Color

class SpellingGameVFX : EventListener {

    var skipNextBallHide = false

    val animations = mutableListOf<Animation>()

    fun run(data: DataPack, game: SpellingGame) {
        val draw = data.bot.draw

        // Draw animations
        val animationIterator = animations.iterator()
        while (animationIterator.hasNext()) {
            val animation = animationIterator.next()
            animation.run(draw)
            if (animation.done) animationIterator.remove()
        }

        // Always draw human first
        val humanIndex = data.allCars.indexOfFirst { it.isHuman }
        if (humanIndex >= 0) {
            val human = game.players[humanIndex]
            var y = 100
            for (objective in human.objectives) {
                draw.string2D(880, y, ALL_WORDS_SHUFFLED[objective], scale = 3)
                y += 60
            }
            draw.string2D(848, y, ">${human.inputBuffer}", scale = 3)
        }

        // Draw pad text
        for (pad in BoostPadManager.allPads) {
            if (pad.active) {
                if (pad.isBig) {
                    draw.string3D(pad.pos + Vec3.UP * 200, "Reset")
                } else {
                    draw.string3D(pad.pos + Vec3.UP * 60, game.padLetters[pad].toString())
                }
            }
        }

        // Hide ball
        if (!skipNextBallHide) {
            RLBotDll.setGameState(
                GameState().withBallState(BallState().withPhysics(PhysicsState().withLocation(Vec3(z = -95).toDesired())))
                    .buildPacket()
            )
        }
        skipNextBallHide = false
    }

    override fun onWordCompleteEvent(event: Event.WordComplete) {
        RLBotDll.setGameState(
            GameState().withBallState(
                BallState().withPhysics(
                    PhysicsState().withLocation(
                        Goal[event.car.team.other()].pos.scaled(1.1f).withZ(200).toDesired()
                    )
                )
            ).buildPacket()
        )
        skipNextBallHide = true

        if (event.car.isHuman) {
            animations.add(Animation(1.2f) { draw, t ->
                draw.string2D(880 + (t * 140f).toInt(), 100 + event.objectiveIndex * 60, event.word, 3, Color.GREEN)
            })
        }
    }
}