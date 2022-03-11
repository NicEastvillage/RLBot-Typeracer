package east.rlbot

import east.rlbot.util.FPS
import rlbot.manager.BotManager


private const val DEFAULT_PORT = 18927

fun main() {
    val pythonInterface = PythonInterface(DEFAULT_PORT, BotManager().also { it.setRefreshRate(FPS) })
    Thread(pythonInterface::start).start()
}
