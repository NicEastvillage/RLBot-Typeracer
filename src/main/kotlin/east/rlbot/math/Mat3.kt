package east.rlbot.math

import org.ejml.data.FMatrixRMaj
import org.ejml.dense.row.CommonOps_FDRM
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class Mat3(internal val internalMat: FMatrixRMaj) {

    constructor(values: Array<FloatArray>): this(FMatrixRMaj(values))
    //constructor(values: FloatArray): this(FMatrixRMaj(values))
    constructor(forward: Vec3, right: Vec3, up: Vec3) : this(arrayOf(
        floatArrayOf(forward.x, right.x, up.x),
        floatArrayOf(forward.y, right.y, up.y),
        floatArrayOf(forward.z, right.z, up.z),
    ))


    fun transpose(): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.transpose(internalMat, ret)
        return Mat3(ret)
    }

    infix fun dot(vec: Vec3): Vec3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.mult(internalMat, toMatrix(vec), ret)
        return toVec(ret)
    }

    infix fun dot(mat: Mat3): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.mult(internalMat, mat.internalMat, ret)
        return Mat3(ret)
    }

    fun trace(): Float {
        return CommonOps_FDRM.trace(internalMat)
    }

    fun get(row: Int, col: Int): Float {
        return internalMat.get(row, col)
    }

    operator fun plus(mat: Mat3): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.add(internalMat, mat.internalMat, ret)
        return Mat3(ret)
    }

    operator fun times(value: Number): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.scale(value.toFloat(), internalMat, ret)
        return Mat3(ret)
    }

    operator fun times(other: Mat3): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.mult(internalMat, other.internalMat, ret)
        return Mat3(ret)
    }


    val forward = Vec3(
                this.internalMat[0, 0],
                this.internalMat[1, 0],
                this.internalMat[2, 0]
        )

    val right = Vec3(
                this.internalMat[0, 1],
                this.internalMat[1, 1],
                this.internalMat[2, 1]
        )

    val up = Vec3(
                this.internalMat[0, 2],
                this.internalMat[1, 2],
                this.internalMat[2, 2]
        )

    /**
     * Returns target as seen from this orientation
     */
    fun toLocal(target: Vec3) = transpose() dot target

    /**
     * Returns local target in global orientation
     */
    fun toGlobal(target: Vec3) = this dot target

    /**
     * https://github.com/samuelpmish/RLUtilities/blob/f071b4dec24d3389f21727ca2d95b75980cbb5fb/RLUtilities/cpp/inc/linalg.h#L71-L74
     */
    fun angleTo(other: Mat3): Float {
        val dot = this.dot(other.transpose())
        val trace = dot.trace()
        return acos(0.5 * (trace - 1)).toFloat()
    }

    fun tr(): Float = internalMat[0, 0] + internalMat[1, 1] + internalMat[2, 2]

    companion object {
        val IDENTITY = Mat3(FMatrixRMaj(arrayOf(floatArrayOf(1F, 0F, 0F), floatArrayOf(0F, 1F, 0F), floatArrayOf(0F, 0F, 1F))))

        internal fun emptyMatrix() = FMatrixRMaj(3, 3)

        internal fun toMatrix(vec: Vec3): FMatrixRMaj {
            return FMatrixRMaj(arrayOf(floatArrayOf(vec.x), floatArrayOf(vec.y), floatArrayOf(vec.z)))
        }

        internal fun toVec(matrix: FMatrixRMaj): Vec3 {
            return Vec3(matrix.get(0), matrix.get(1), matrix.get(2))
        }

        fun lookingAt(from: Vec3, target: Vec3, up: Vec3 = Vec3.UP): Mat3 {
            return lookingInDir(target - from, up)
        }

        fun lookingInDir(direction: Vec3, up: Vec3 = Vec3.UP): Mat3 {
            val forward = direction.dir()
            val safeUp = if (abs(forward.z) == 1F && abs(up.z) == 1F) Vec3(x = 1.0) else up
            val right = (safeUp cross forward).dir()
            val upward = (forward cross right).dir()

            return Mat3(forward, right, upward)
        }

        fun rotationMatrix(unitAxis: Vec3, rad: Float): Mat3 {
            val cosTheta = cos(rad)
            val sinTheta = sin(rad)
            val n1CosTheta = 1F - cosTheta
            val u = unitAxis
            return Mat3(arrayOf(
                    floatArrayOf(cosTheta + u.x * u.x * n1CosTheta, u.x * u.y * n1CosTheta - u.z * sinTheta, u.x * u.z * n1CosTheta + u.y * sinTheta),
                    floatArrayOf(u.y * u.x * n1CosTheta + u.z * sinTheta, cosTheta + u.y * u.y * n1CosTheta, u.y * u.z * n1CosTheta - u.x * sinTheta),
                    floatArrayOf(u.z * u.x * n1CosTheta - u.y * sinTheta, u.z * u.y * n1CosTheta + u.x * sinTheta, cosTheta + u.z * u.z * n1CosTheta)
            ))
        }

        fun eulerToRotation(pitch: Float, yaw: Float, roll: Float): Mat3 {

            val CP = cos(pitch)
            val SP = sin(pitch)
            val CY = cos(yaw)
            val SY = sin(yaw)
            val CR = cos(roll)
            val SR = sin(roll)

            val noseX = CP * CY
            val noseY = CP * SY
            val noseZ = SP

            val roofX = -CR * CY * SP - SR * SY
            val roofY = -CR * SY * SP + SR * CY
            val roofZ = CP * CR

            return lookingInDir(Vec3(noseX, noseY, noseZ), up = Vec3(roofX, roofY, roofZ))
        }
    }
}