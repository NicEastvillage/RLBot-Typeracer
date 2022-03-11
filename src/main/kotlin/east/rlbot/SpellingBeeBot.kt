package east.rlbot

class SpellingBeeBot(index: Int, team: Int, name: String) : BaseBot(index, team, name) {

    override fun onKickoffBegin() {

    }

    override fun getOutput(): OutputController {
        return OutputController().withThrottle(1)
    }
}