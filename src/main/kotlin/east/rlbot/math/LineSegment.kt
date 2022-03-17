package east.rlbot.math

data class LineSegment(
    val start: Vec3,
    val end: Vec3,
) {
    fun length() = start.dist(end)
    fun lengthSqr() = start.distSqr(end)

    fun eval(t: Float) = start.lerp(end, t)
}