package east.rlbot

import rlbot.Bot
import rlbot.manager.BotManager
import rlbot.pyinterop.SocketServer

class PythonInterface(port: Int, botManager: BotManager) : SocketServer(port, botManager) {

    override fun initBot(index: Int, botType: String, team: Int): Bot {
        return TyperRacerBot(index, team, botType)
    }
}