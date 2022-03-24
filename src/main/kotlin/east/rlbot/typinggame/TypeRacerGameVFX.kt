package east.rlbot.typinggame

import east.rlbot.data.BoostPadManager
import east.rlbot.data.DataPack
import east.rlbot.data.Goal
import east.rlbot.math.Vec3
import east.rlbot.util.PIf
import east.rlbot.util.pingPong
import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.BallState
import rlbot.gamestate.GameState
import rlbot.gamestate.PhysicsState
import java.awt.Color
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class TypeRacerGameVFX : EventListener {

    private val humanInfoX = 880
    private val humanInfoY = 100
    private val botInfoX = 50
    private val botInfoY = 140
    private val infoYDeltaPerScale = 20
    private val charWidthPerScale = 11

    var showTutorial = true
    var skipNextBallHide = false
    var humanInputShorten = 0
    var humanInput = ""

    val animations = mutableListOf<Animation>()
    val queuedAnimations = mutableListOf<Animation>()

    fun run(data: DataPack, game: TypeRacerGame) {
        val draw = data.bot.draw

        // Draw animations
        val animationIterator = animations.iterator()
        while (animationIterator.hasNext()) {
            val animation = animationIterator.next()
            animation.run(draw)
            if (animation.done) {
                animationIterator.remove()
                animation.onFinish?.invoke()
            }
        }
        animations += queuedAnimations
        queuedAnimations.clear()

        // Always draw human first
        draw.color = Color.WHITE
        val humanIndex = data.allCars.indexOfFirst { it.isHuman }
        var scale = 3
        if (humanIndex >= 0) {
            val human = game.players[humanIndex]
            var y = humanInfoY
            for (objective in human.objectives) {
                draw.string2D(humanInfoX, y, ALL_WORDS_SHUFFLED[objective], scale)
                y += infoYDeltaPerScale * scale
            }
            draw.string2D(humanInfoX - charWidthPerScale * scale, y, ">${humanInput.dropLast(humanInputShorten)}", scale)

            if (showTutorial) {
                draw.color = Color.YELLOW
                scale = 2
                draw.string2D(
                    humanInfoX + 265,
                    humanInfoY,
                    "Write any of these words to\nscore points. Type by picking\nup small boost pads.",
                    scale
                )
                draw.string2D(
                    humanInfoX + 190 + (data.match.time * 65).pingPong(1.5f * charWidthPerScale * scale).toInt(),
                    humanInfoY,
                    "<\n<\n<",
                    scale
                )
            }
        }

        draw.color = Color.WHITE
        scale = 2
        var y = botInfoY
        for ((i, bot) in data.allCars.withIndex().filter { !it.value.isHuman }) {
            draw.string2D(botInfoX, y, "${bot.name}:", scale, bot.team.color)
            y += infoYDeltaPerScale * scale
            val player = game.players[i]
            for (objective in player.objectives) {
                draw.string2D(botInfoX, y, ALL_WORDS_SHUFFLED[objective], scale)
                y += infoYDeltaPerScale * scale
            }
            draw.string2D(botInfoX - charWidthPerScale * scale, y, ">${player.inputBuffer}", scale)
            y += 2 * infoYDeltaPerScale * scale
        }

        // Draw pad text
        draw.color = Color.WHITE
        for (pad in BoostPadManager.allPads) {
            if (pad.active) {
                if (pad.isBig) {
                    draw.string3D(pad.pos + Vec3.UP * 200, "Reset", scale=2)
                } else {
                    draw.string3D(pad.pos + Vec3.UP * 60, game.padLetters[pad].toString(), scale=2)
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
        if (event.car.isHuman) showTutorial = false

        val (x, y, scale) = if (event.car.isHuman) {
            val scale = 3
            Triple(humanInfoX, humanInfoY + event.objectiveIndex * infoYDeltaPerScale * scale, scale)
        } else {
            val scale = 2
            val botIndex = event.car.index - 1 // Assuming human is 0
            Triple(botInfoX, botInfoY + botIndex * infoYDeltaPerScale * scale * 7 + (event.objectiveIndex + 1) * infoYDeltaPerScale * scale, scale)
        }

        val ani = Animation(1.2f) { draw, t ->
            draw.string2D(x + (t * scale * 46f).toInt(), y, event.word, scale, Color.GREEN)
        }

        if (event.car.isHuman) {
            // Wait for letter animation
            animations.add(Animation(0.27f) { _, _ -> }.onFinish {
                humanInput = ""
                queuedAnimations.add(ani)
                for ((i, letter) in event.word.withIndex()) {
                    val pos0 = Vec3(humanInfoX + charWidthPerScale * 3 * i, humanInfoY + infoYDeltaPerScale * scale * 4)
                    val vel0 = Vec3(x=sin(Random.nextFloat() * PIf) * 200, y=Random.nextFloat() * -70) + Vec3(y=-150)
                    val acc = Vec3(y=600)
                    queuedAnimations.add(Animation(1.5f) { draw, t ->
                        val rt = t * 1.5f
                        val pos = acc * rt.pow(2) / 2f + vel0 * rt + pos0
                        draw.string2D(pos.x.toInt(), pos.y.toInt(), letter.toString(), scale, Color.GREEN)
                    })
                }
            })
        } else {
            animations.add(ani)
        }
    }

    override fun onLetterPickupEvent(event: Event.LetterPickup) {
        if (event.car.isHuman) {
            humanInput = event.input
            humanInputShorten++
            animations.add(Animation(0.27f) { draw, t ->
                val scale = 3
                val end = Vec3(humanInfoX + charWidthPerScale * scale * (event.input.length - 1), humanInfoY + infoYDeltaPerScale * scale * 4)
                val pos = Vec3(930, 600).lerp(end, t)
                draw.string2D(pos.x.toInt(), pos.y.toInt(), event.letter.toString(), scale, Color.WHITE)
            }.onFinish {
                humanInputShorten--
            })
        }
    }

    override fun onResetPickupEvent(event: Event.ResetPickup) {
        if (event.car.isHuman) {
            humanInput = ""
            animations.add(Animation(0.4f) { draw, t ->
                val scale = 3
                draw.string2D(humanInfoX + (10 * sin(t * 40)).toInt(), humanInfoY + infoYDeltaPerScale * scale * 3, event.input, scale, Color.RED)
            })
        }
    }
}