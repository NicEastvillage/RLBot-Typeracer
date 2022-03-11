package east.rlbot.math

import east.rlbot.data.Goal

class AimCone(
    centerDir: Vec3,
    val angle: Float,
) {
    val centerDir = centerDir.dir()

    fun contains(dir: Vec3) = centerDir.angle(dir) <= angle

    fun clamp(dir: Vec3): Vec3 {
        val udir = dir.dir()
        val delta = centerDir.angle(dir)
        if (delta <= angle) return dir
        val adjustAng = delta - angle
        val axis = udir.cross(centerDir)
        val rot = Mat3.rotationMatrix(axis, adjustAng)
        return (rot dot udir) * dir.mag()
    }

    fun withAngle(angle: Float) = AimCone(centerDir, angle)

    companion object {
        fun atGoal(from: Vec3, goal: Goal): AimCone {
            val dirToRight = from.dirTo2D(goal.rightPadded)
            val dirToLeft = from.dirTo2D(goal.leftPadded)
            var centralDir = (dirToRight + dirToLeft) / 2f
            if (centralDir == Vec3.ZERO) centralDir = dirToRight.perp2D()
            val angle = centralDir.angle(dirToRight)
            return AimCone(centralDir, angle)
        }
    }
}