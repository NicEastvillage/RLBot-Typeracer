package east.rlbot

import east.rlbot.math.clamp
import rlbot.ControllerState

class OutputController : ControllerState {

    var steer: Double = 0.0
        private set

    var pitch: Double = 0.0
        private set

    var yaw: Double = 0.0
        private set

    var roll: Double = 0.0
        private set

    var throttle: Double = 0.0
        private set

    var jumpDepressed: Boolean = false
        private set

    var boostDepressed: Boolean = false
        private set

    var slideDepressed: Boolean = false
        private set

    var useItemDepressed: Boolean = false
        private set

    fun withSteer(steer: Number): OutputController {
        this.steer = clamp(steer.toDouble(), -1.0, 1.0)
        return this
    }

    fun withPitch(pitch: Number): OutputController {
        this.pitch = clamp(pitch.toDouble(), -1.0, 1.0)
        return this
    }

    fun withYaw(yaw: Number): OutputController {
        this.yaw = clamp(yaw.toDouble(), -1.0, 1.0)
        return this
    }

    fun withRoll(roll: Number): OutputController {
        this.roll = clamp(roll.toDouble(), -1.0, 1.0)
        return this
    }

    fun withThrottle(throttle: Number): OutputController {
        this.throttle = clamp(throttle.toDouble(), -1.0, 1.0)
        return this
    }

    fun withJump(jumpDepressed: Boolean): OutputController {
        this.jumpDepressed = jumpDepressed
        return this
    }

    fun withBoost(boostDepressed: Boolean): OutputController {
        this.boostDepressed = boostDepressed
        return this
    }

    fun withSlide(slideDepressed: Boolean): OutputController {
        this.slideDepressed = slideDepressed
        return this
    }

    fun withUseItem(useItemDepressed: Boolean): OutputController {
        this.useItemDepressed = useItemDepressed
        return this
    }

    fun withJump(): OutputController {
        this.jumpDepressed = true
        return this
    }

    fun withBoost(): OutputController {
        this.boostDepressed = true
        return this
    }

    fun withSlide(): OutputController {
        this.slideDepressed = true
        return this
    }

    fun withUseItem(): OutputController {
        this.useItemDepressed = true
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutputController

        if (steer != other.steer) return false
        if (pitch != other.pitch) return false
        if (yaw != other.yaw) return false
        if (roll != other.roll) return false
        if (throttle != other.throttle) return false
        if (jumpDepressed != other.jumpDepressed) return false
        if (boostDepressed != other.boostDepressed) return false
        if (slideDepressed != other.slideDepressed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = steer.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + roll.hashCode()
        result = 31 * result + throttle.hashCode()
        result = 31 * result + jumpDepressed.hashCode()
        result = 31 * result + boostDepressed.hashCode()
        result = 31 * result + slideDepressed.hashCode()
        return result
    }

    override fun getYaw(): Float {
        return yaw.toFloat()
    }

    override fun getSteer(): Float {
        return steer.toFloat()
    }

    override fun getThrottle(): Float {
        return throttle.toFloat()
    }

    override fun getPitch(): Float {
        return pitch.toFloat()
    }

    override fun getRoll(): Float {
        return roll.toFloat()
    }

    override fun holdHandbrake(): Boolean {
        return slideDepressed
    }

    override fun holdBoost(): Boolean {
        return boostDepressed
    }

    override fun holdJump(): Boolean {
        return jumpDepressed
    }

    override fun holdUseItem(): Boolean {
        return useItemDepressed
    }
}