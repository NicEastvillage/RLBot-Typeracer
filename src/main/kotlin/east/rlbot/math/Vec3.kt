package east.rlbot.math

import east.rlbot.util.coerceIn01
import org.ejml.dense.row.CommonOps_FDRM
import rlbot.gamestate.DesiredVector3
import kotlin.math.*
import kotlin.random.Random

class Vec3(x: Number = 0, y: Number = 0, z: Number = 0): rlbot.vector.Vector3(x.toFloat(), y.toFloat(), z.toFloat()) {

    constructor(flatVec: rlbot.flat.Vector3): this(flatVec.x(), flatVec.y(), flatVec.z())

    val isZero = mag() < 1e-07

    operator fun plus(other: Vec3): Vec3 {
        return Vec3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vec3): Vec3 {
        return Vec3(x - other.x, y - other.y, z - other.z)
    }

    operator fun div(value: Number): Vec3 {
        return Vec3(x / value.toFloat(), y / value.toFloat(), z / value.toFloat())
    }

    operator fun div(value: Vec3): Vec3 {
        return Vec3(x / value.x, y / value.y, z / value.z)
    }

    operator fun times(value: Number): Vec3 {
        return Vec3(x * value.toFloat(), y * value.toFloat(), z * value.toFloat())
    }

    operator fun times(value: Vec3): Vec3 {
        return Vec3(x * value.x, y * value.y, z * value.z)
    }

    operator fun get(index: Int): Float {
        if (index == 0)
            return x
        if (index == 1)
            return y
        if (index == 2)
            return z
        return 0F
    }

    fun scaled(scale: Number): Vec3 {
        val s = scale.toFloat()
        return Vec3(x * s, y * s, z * s)
    }

    fun withX(x: Number): Vec3 {
        return Vec3(x, y, z)
    }

    fun withY(y: Number): Vec3 {
        return Vec3(x, y, z)
    }

    fun withZ(z: Number): Vec3 {
        return Vec3(x, y, z)
    }

    fun scaledToMag(magnitude: Number): Vec3 {
        if (isZero) {
            throw IllegalStateException("Cannot scale up a vector with length zero!")
        }
        val scaleRequired = magnitude.toFloat() / mag()
        return scaled(scaleRequired)
    }

    fun dist(other: Vec3): Float {
        return sqrt(distSqr(other))
    }

    fun distSqr(other: Vec3): Float {
        val xDiff = x - other.x
        val yDiff = y - other.y
        val zDiff = z - other.z
        return (xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }

    fun dist2D(other: Vec3): Float {
        return sqrt(dist2DSqr(other))
    }

    fun dist2DSqr(other: Vec3): Float {
        val xDiff = x - other.x
        val yDiff = y - other.y
        return (xDiff * xDiff + yDiff * yDiff)
    }

    fun mag(): Float {
        return sqrt(magSqr())
    }

    fun magSqr(): Float {
        return (x * x + y * y + z * z)
    }

    fun dir(): Vec3 {
        if (isZero) {
            throw IllegalStateException("Cannot normalize a vector with length zero!")
        }
        return this.scaled(1 / mag())
    }

    fun dirTo(other: Vec3) = (other - this).dir()

    fun dir2D(): Vec3 = flat().dir()

    fun dirTo2D(other: Vec3) = (other - this).flat().dir()

    infix fun dot(other: Vec3): Float {
        return x * other.x + y * other.y + z * other.z
    }

    infix fun dot(mat: Mat3): Vec3 {
        val ret = Mat3.emptyMatrix()
        val asMat = Mat3.toMatrix(this)
        CommonOps_FDRM.transpose(asMat)
        CommonOps_FDRM.mult(asMat, mat.internalMat, ret)
        return Mat3.toVec(ret)
    }

    fun flat(): Vec3 {
        return withZ(0)
    }

    fun angle(other: Vec3): Float {
        val mag2 = magSqr()
        val vmag2 = other.magSqr()
        val dot = this dot other
        return acos(dot / sqrt(mag2 * vmag2))
    }

    fun angle2D(other: Vec3): Float {
        return flat().angle(other.flat())
    }

    infix fun cross(v: Vec3): Vec3 {
        val tx = y * v.z - z * v.y
        val ty = z * v.x - x * v.z
        val tz = x * v.y - y * v.x
        return Vec3(tx, ty, tz)
    }

    fun perp2D(): Vec3 = Vec3(-y, x, z)

    fun rotate2D(angle: Float) = Vec3(cos(angle) * x - sin(angle) * y, sin(angle) * x + cos(angle) * y, z)

    fun rotate(axis: Vec3, angle: Float) = Mat3.rotationMatrix(axis.dir(), angle) dot this

    fun dist(plane: Plane): Float {
        return abs((this - plane.offset) dot plane.normal)
    }

    fun dist(line: LineSegment): Float {
        val lenSqr = line.lengthSqr()
        if (lenSqr < 0.0001f) return dist(line.start)
        val t = ((this - line.start) dot (line.end - line.start) / lenSqr).coerceIn01()
        return dist(line.eval(t))
    }

    fun projectToPlane(planeNormal: Vec3): Vec3 {
        val distance = this dot planeNormal
        val antidote = planeNormal.scaled(-distance)
        return plus(antidote)
    }

    fun projectToPlane(plane: Plane): Vec3 {
        return (this - plane.offset).projectToPlane(plane.normal) + plane.offset
    }

    fun projectToNearest(planes: List<Plane>): Vec3 {
        val closest = planes.minByOrNull { dist(it) }!!
        return projectToPlane(closest)
    }

    fun lerp(other: Vec3, t: Float) = (1f - t) * this + t * other

    fun abs(): Vec3 {
        return Vec3(abs(x), abs(y), abs(z))
    }

    fun maxComponent(): Float = max(max(x, y), z)
    fun minComponent(): Float = min(min(x, y), z)

    fun coerceAtLeast(minValues: Vec3) = Vec3(
        max(x, minValues.x),
        max(y, minValues.y),
        max(z, minValues.z),
    )

    fun coerceAtMost(maxValues: Vec3) = Vec3(
        min(x, maxValues.x),
        min(y, maxValues.y),
        min(z, maxValues.z),
    )

    fun coerceIn(minValues: Vec3, maxValues: Vec3) = Vec3(
        x.coerceIn(minValues.x, maxValues.x),
        y.coerceIn(minValues.y, maxValues.y),
        z.coerceIn(minValues.z, maxValues.z),
    )

    fun toDesired(): DesiredVector3 = DesiredVector3(x, y, z)

    override fun toString(): String {
        return "(%.2f, %.2f, %.2f)".format(x, y, z)
    }

    override fun equals(other: Any?): Boolean {
        val o = other as? Vec3 ?: return false
        return o.x == x && o.y == y && o.z == z
    }

    /**
     * Returns angle between this and Vec(1, 0, 0)
     */
    fun atan2(): Float = Math.atan2(y.toDouble(), x.toDouble()).toFloat()

    companion object {
        val UP = Vec3(0.0, 0.0, 1.0)
        val DOWN = Vec3(0.0, 0.0, -1.0)
        val ZERO = Vec3()
        val ONES = Vec3(1f, 1f ,1f)

        fun random(): Vec3 = Vec3(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())

        fun dirFromAng2D(angle: Float) = Vec3(cos(angle), sin(angle))
    }
}
