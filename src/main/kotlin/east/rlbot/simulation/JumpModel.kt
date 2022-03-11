package east.rlbot.simulation

import east.rlbot.data.Car

object JumpModel {
    val single = generate(doDoubleJump = false)
    val double = generate(doDoubleJump = true)

    private fun generate(doDoubleJump: Boolean): JumpLUT {
        val dt = 1/120f

        var time = 0f
        var velZ = 0f
        var prevHeight = Car.REST_HEIGHT
        var hasDoubleJumped = false

        val entries = mutableListOf(prevHeight)

        while (true) {
            // Jumping
            if (time == 0f) {
                velZ += Car.JUMP_IMPULSE

            } else if (time > Car.MAX_JUMP_HOLD_TIME + dt && doDoubleJump && !hasDoubleJumped) {
                velZ += Car.JUMP_IMPULSE
                hasDoubleJumped = true
            }

            // Hold force
            if (time < Car.MAX_JUMP_HOLD_TIME)
                velZ += Car.JUMP_HOLD_FORCE * dt

            // Wall stickiness
            if (time < Car.WALL_STICKY_TIME)
                velZ -= Car.WALL_STICKY_FORCE * dt

            velZ += Physics.GRAVITY.z * dt

            val newHeight = prevHeight + velZ * dt
            time += dt

            // Break when we start falling
            if (newHeight < prevHeight) break

            entries.add(newHeight)
            prevHeight = newHeight
        }

        return JumpLUT(entries, dt)
    }
}