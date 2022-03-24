package east.rlbot.typinggame

import east.rlbot.data.BoostPad
import east.rlbot.data.Car

sealed class Event {
    data class ResetPickup(val car: Car, val pad: BoostPad, val input: String) : Event()
    data class LetterPickup(val car: Car, val pad: BoostPad, val letter: Char, val input: String) : Event()
    data class WordComplete(val car: Car, val pad: BoostPad, val word: String, val newScore: Int, val objectiveIndex: Int, val newObjective: String) : Event()
    data class SmallPadRespawn(val pad: BoostPad, val newLetter: Char)
    data class BigPadRespawn(val pad: BoostPad)
}

interface EventListener {
    fun onResetPickupEvent(event: Event.ResetPickup) {}
    fun onLetterPickupEvent(event: Event.LetterPickup) {}
    fun onWordCompleteEvent(event: Event.WordComplete) {}
    fun onSmallPadRespawnEvent(event: Event.SmallPadRespawn) {}
    fun onBigPadRespawnEvent(event: Event.BigPadRespawn) {}
}
