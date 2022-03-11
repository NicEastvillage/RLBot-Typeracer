package east.rlbot.util

import com.google.flatbuffers.FlatBufferBuilder
import rlbot.cppinterop.RLBotDll
import rlbot.render.RenderPacket
import rlbot.render.Renderer


/**
 * A renderer. Remember to call `startPacket` and
 * `finishAndSendIfDifferent`. You cannot use this and the `BotLoopRenderer` at the same time.
 */
class BasicRenderer(index: Int) : Renderer(index) {

    private var previousPacket: RenderPacket? = null

    fun startPacket() {
        builder = FlatBufferBuilder(1000)
    }

    fun finishAndSendIfDifferent() {
        val packet: RenderPacket = doFinishPacket()
        if (packet != previousPacket) {
            RLBotDll.sendRenderPacket(packet)
            previousPacket = packet
        }
    }
}