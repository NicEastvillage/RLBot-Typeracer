package east.rlbot.spellinggame

import east.rlbot.util.DT
import east.rlbot.util.DebugDraw

class Animation(val duration: Float, val animation: (DebugDraw, Float) -> Unit) {
    var progress = 0f
    var done = false
    var onFinish: (() -> Unit)? = null
    fun run(draw: DebugDraw) {
        progress += DT
        if (progress >= duration) done = true
        animation(draw, progress / duration)
    }
    fun onFinish(finish: () -> Unit): Animation {
        onFinish = finish
        return this
    }
}