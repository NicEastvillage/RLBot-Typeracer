package east.rlbot.spellinggame

import east.rlbot.data.BoostPad
import east.rlbot.data.BoostPadManager
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.math.Vec3
import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.CarState
import rlbot.gamestate.GameState

class SpellingGame(val playerCount: Int, val concurrentWords: Int = 4) {

    val players = Array(playerCount) { i -> Player(i, concurrentWords) }
    val letterCount = LETTERS.associateWith { 0 }.toMutableMap()
    val padLetters = BoostPadManager.smallPads.associateWith { semiRandomLetter() }.toMutableMap()

    fun run(data: DataPack) {
        // Check if car is close to active boost pad (Use boost to detect touch)
        for (car in data.allCars) {
            if (car.boost > 70) {
                // This car must have picked up a boost pad
                val closest = BoostPadManager.allPads.minByOrNull { car.pos.dist(it.pos) }
                if (closest != null) {
                    if (closest.isBig)
                        onBigPadPickup(closest, car)
                    else
                        onSmallPadPickup(closest, car)
                }
            }
        }

        // Set all cars' boost to 69%
        val gameState = GameState()
        for (car in data.allCars) {
            val carState = CarState().withBoostAmount(69f)
            gameState.withCarState(car.index, carState)
        }
        RLBotDll.setGameState(gameState.buildPacket())

        // display small pad letters, current objectives, scores, and info
        draw(data)
    }

    private fun semiRandomLetter(): Char {
        // If all letters appear at least once, we add an extra of a common letter
        val missingLetters = letterCount.entries.filter { it.value == 0 }.map { it.key }.toList()
        val picked = if (missingLetters.isNotEmpty()) missingLetters.random() else COMMON_LETTERS.random()
        letterCount[picked] = letterCount[picked]!! + 1
        return picked
    }

    private fun releaseLetter(letter: Char) {
        letterCount[letter] = letterCount[letter]!! - 1
    }

    private fun onSmallPadPickup(pad: BoostPad, car: Car) {
        assert(!pad.isBig)
        val player = players[car.index]
        player.inputBuffer += padLetters[pad]
        releaseLetter(padLetters[pad]!!)
        padLetters[pad] = semiRandomLetter()

        val completedObjectiveIndex = player.objectives
            .map { ALL_WORDS_SHUFFLED[it] }
            .indexOfFirst { it == player.inputBuffer }
        println("$completedObjectiveIndex")
        if (completedObjectiveIndex >= 0) {
            player.score++
            player.objectives[completedObjectiveIndex] = player.nextObjective
            player.nextObjective++
            player.inputBuffer = ""
        }
    }

    private fun onBigPadPickup(pad: BoostPad, car: Car) {
        assert(pad.isBig)
        players[car.index].inputBuffer = ""
    }

    private fun draw(data: DataPack) {
        val draw = data.bot.draw

        // Always draw human first
        val humanIndex = data.allCars.indexOfFirst { it.isHuman }
        if (humanIndex >= 0) {
            val human = players[humanIndex]
            var y = 20
            for (objective in human.objectives) {
                draw.string2D(20, y, ALL_WORDS_SHUFFLED[objective])
                y += 20
            }
            draw.string2D(20, y, ">${human.inputBuffer}")
        }

        // Draw pad text
        for (pad in BoostPadManager.allPads) {
            if (pad.active) {
                if (pad.isBig) {
                    draw.string3D(pad.pos + Vec3.UP * 200, "Reset")
                } else {
                    draw.string3D(pad.pos + Vec3.UP * 60, padLetters[pad].toString())
                }
            }
        }
    }
}