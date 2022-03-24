package east.rlbot.typinggame

import java.util.logging.Level
import java.util.logging.Logger

class GameLog(game: TypeRacerGame) : EventListener {

    val logger: Logger

    init {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1\$tT] [%4\$s] %5\$s %n");
        logger = Logger.getLogger(GameLog::class.simpleName)
        game.eventListeners.add(this)
    }

    override fun onResetPickupEvent(event: Event.ResetPickup) {
        logger.log(Level.INFO, "${event.car.name} picked up reset (big pad with index ${event.pad.index}).")
    }

    override fun onLetterPickupEvent(event: Event.LetterPickup) {
        logger.log(Level.INFO, "${event.car.name} picked up letter '${event.letter}' so their input is '${event.input}' (small pad with index ${event.pad.index}).")
    }

    override fun onWordCompleteEvent(event: Event.WordComplete) {
        logger.log(Level.INFO, "${event.car.name} completed word '${event.word}'. Their score is now ${event.newScore}.")
    }

    override fun onSmallPadRespawnEvent(event: Event.SmallPadRespawn) {
        logger.log(Level.INFO, "Small pad with index ${event.pad.index} respawned with letter '${event.newLetter}'.")
    }

    override fun onBigPadRespawnEvent(event: Event.BigPadRespawn) {
        logger.log(Level.INFO, "Big pad with index ${event.pad.index} respawned.")
    }
}