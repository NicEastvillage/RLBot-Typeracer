package east.rlbot.spellinggame

import east.rlbot.data.BoostPad
import east.rlbot.data.BoostPadManager
import east.rlbot.data.Car
import east.rlbot.data.DataPack
import east.rlbot.math.Vec3
import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.CarState
import rlbot.gamestate.GameState

class SpellingGame(val playerCount: Int, val concurrentWords: Int = 3) {

    data class PadRespawn(val pad: BoostPad, val time: Float)

    val players = Array(playerCount) { i -> Player(i, concurrentWords) }
    val letterCount = LETTERS.associateWith { 0 }.toMutableMap()
    val padLetters = BoostPadManager.smallPads.associateWith { semiRandomLetter() }.toMutableMap()
    val padRespawns = mutableListOf<PadRespawn>()
    val eventListeners = mutableListOf<EventListener>()

    fun run(data: DataPack) {
        // Handle respawning pads
        val padRespawnsIterator = padRespawns.iterator()
        while (padRespawnsIterator.hasNext()) {
            val res = padRespawnsIterator.next()
            if (data.match.time < res.time) continue
            padRespawnsIterator.remove()
            if (res.pad.isBig) {
                val event = Event.BigPadRespawn(res.pad)
                eventListeners.forEach { it.onBigPadRespawnEvent(event) }
            } else {
                val newLetter = semiRandomLetter()
                padLetters[res.pad] = newLetter
                val event = Event.SmallPadRespawn(res.pad, newLetter)
                eventListeners.forEach { it.onSmallPadRespawnEvent(event) }
            }
        }

        // Check if car is close to active boost pad (Use boost to detect touch)
        for (car in data.allCars) {
            if (car.boost > 70) {
                // This car must have picked up a boost pad
                val closest = BoostPadManager.allPads.minByOrNull { car.pos.dist(it.pos) }
                if (closest != null) {
                    if (closest.isBig)
                        onBigPadPickup(data.match.time, closest, car)
                    else
                        onSmallPadPickup(data.match.time, closest, car)
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

    private fun onSmallPadPickup(time: Float, pad: BoostPad, car: Car) {
        assert(!pad.isBig)
        val player = players[car.index]
        val letter = padLetters[pad]!!
        player.inputBuffer += letter
        releaseLetter(padLetters[pad]!!)
        padLetters[pad] = '-'
        padRespawns.add(PadRespawn(pad, time + pad.refreshTime))
        val letterPickupEvent = Event.LetterPickup(car, pad, letter, player.inputBuffer)
        eventListeners.forEach { it.onLetterPickupEvent(letterPickupEvent) }

        val completedObjectiveIndex = player.objectives
            .map { ALL_WORDS_SHUFFLED[it] }
            .indexOfFirst { it == player.inputBuffer }
        if (completedObjectiveIndex >= 0) {
            val completedWord = ALL_WORDS_SHUFFLED[player.objectives[completedObjectiveIndex]]
            player.score++
            player.objectives[completedObjectiveIndex] = player.nextObjective
            player.nextObjective++
            player.inputBuffer = ""
            val wordCompleteEvent = Event.WordComplete(car, pad, completedWord, player.score, completedObjectiveIndex, ALL_WORDS_SHUFFLED[player.objectives[completedObjectiveIndex]])
            eventListeners.forEach { it.onWordCompleteEvent(wordCompleteEvent) }
        }
    }

    private fun onBigPadPickup(time: Float, pad: BoostPad, car: Car) {
        assert(pad.isBig)
        val oldInput = players[car.index].inputBuffer
        players[car.index].inputBuffer = ""
        padRespawns.add(PadRespawn(pad, time + pad.refreshTime))
        eventListeners.forEach { it.onResetPickupEvent(Event.ResetPickup(car, pad, oldInput)) }
    }

    private fun draw(data: DataPack) {
        val draw = data.bot.draw

        // Always draw human first
        val humanIndex = data.allCars.indexOfFirst { it.isHuman }
        if (humanIndex >= 0) {
            val human = players[humanIndex]
            var y = 100
            for (objective in human.objectives) {
                draw.string2D(880, y, ALL_WORDS_SHUFFLED[objective], scale = 3)
                y += 60
            }
            draw.string2D(850, y, ">${human.inputBuffer}", scale = 3)
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