package east.rlbot.math

import east.rlbot.data.Car
import east.rlbot.data.FutureBall
import east.rlbot.simulation.*
import east.rlbot.util.DebugDraw
import east.rlbot.util.PIf
import east.rlbot.util.half
import java.awt.Color
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sign
import kotlin.math.sqrt

class ArcLineArc(
    val start: Vec3,
    val startDir: Vec3,
    val end: Vec3,
    val endDir: Vec3,
    val radius1: Float,
    val radius2: Float,
) {
    val circle1Center: Vec3
    val circle2Center: Vec3
    val startDirNormal: Vec3
    val endDirNormal: Vec3
    val tangentPoint1: Vec3
    val tangentPoint2: Vec3

    val angle1: Float
    val angle2: Float
    val length: Float
    val arc1Length: Float
    val straightLength: Float
    val arc2Length: Float

    init {
        val l1 = 1f
        val l2 = 1f

        val p1 = start + l1 * startDir
        startDirNormal = startDir.perp2D()
        circle1Center = p1 + startDirNormal * radius1

        val p2 = end - endDir * l2
        endDirNormal = endDir.perp2D()
        circle2Center = p2 + endDirNormal * radius2

        val centerDelta = circle2Center - circle1Center

        // Figure out if we transition from CW to CCW or vice versa
        // and compute some of the characteristic lengths for the problem
        val sign = -radius1.sign * radius2.sign
        val R = abs(radius1) + sign * abs(radius2)
        val centerDist = centerDelta.mag()
        val beta = 0.97f

        // Resize the radii if the circles are too close
//        if (R * R / (centerDist * centerDist) > beta) {
//            val deltaP = p2 - p1
//            val deltaN = (endDirNormal * radius2) - (startDirNormal * radius1)
//
//            val a = beta * deltaN.dot(deltaN) - R * R
//            val b = 2.0f * beta * deltaN.dot(deltaP)
//            val c = beta * deltaP.dot(deltaP)
//            val alpha = (-b - sqrt(b * b - 4.0f * a * c)) / (2.0f * a)
//
//            // Scale the radii by alpha, and update the relevant quantities
//            radius1 *= alpha
//            radius2 *= alpha
//            R *= alpha
//            circle1Center = p1 + startDirNormal * radius1
//            circle2Center = p2 + endDirNormal * radius2
//            centerDelta = circle2Center - circle1Center
//            centerDist = centerDelta.mag()
//        }

        // Set up a coordinate system along the axis
        // connecting the two circle's centers
        val e1 = centerDelta.dir()
        val e2 = -sign(radius1) * e1.perp2D()

        val H = sqrt(centerDist * centerDist - R * R)

        // The endpoints of the line segment connecting the circles
        tangentPoint1 = circle1Center + (e1 * (R / centerDist) + e2 * (H / centerDist)) * abs(radius1)
        tangentPoint2 = circle2Center - (e1 * (R / centerDist) + e2 * (H / centerDist)) * abs(radius2) * sign

        val pq1 = (tangentPoint1 - p1).dir()
        var _angle1 = 2.0f * sign(pq1 dot startDir) * asin(abs(pq1 dot startDirNormal))
        if (_angle1 < 0.0f) _angle1 += 2.0f * PIf
        angle1 = _angle1

        val pq2 = (tangentPoint2 - p2).dir()
        var _angle2 = -2.0f * sign(pq2 dot endDir) * asin(abs(pq2 dot endDirNormal))
        if (_angle2 < 0.0f) _angle2 += 2.0f * PIf
        angle2 = _angle2

        arc1Length = angle1 * abs(radius1)
        straightLength = tangentPoint2.dist(tangentPoint1)
        arc2Length = angle2 * abs(radius2)
        length = arc1Length + straightLength + arc2Length
    }

    companion object {
        fun findShortest(
            start: Vec3,
            startDir: Vec3,
            end: Vec3,
            endDir: Vec3,
            radius1: Float,
            radius2: Float,
        ): ArcLineArc {
            var best: ArcLineArc? = null
            for (s1 in listOf(1f, -1f)) {
                for (s2 in listOf(1f, -1f)) {
                    val curve = ArcLineArc(start.flat(), startDir, end, endDir, s1 * radius1, s2 * radius2)
                    if (best == null || best.length > curve.length) best = curve
                }
            }
            return best!!
        }

        /**
         * Finds an ArcLineArc that hits the ball in the target direction, considering acceleration and more
         */
        @Deprecated("Experimental")
        fun findSmart(
            car: Car,
            ball: FutureBall,
            desiredVel: Vec3,
            carRadius: Float,
            iterations: Int = 100,
            draw: DebugDraw?,
        ): ArcLineArc {
            val startDir = car.ori.forward.flat().dir()
            val hitParam = findHitParameters(ball, desiredVel, carRadius)
            val curForwardSpeed = car.forwardSpeed().coerceAtLeast(300f)
            val radius1 = turnRadius(curForwardSpeed)
            val initALA = findShortest(car.pos, startDir, hitParam.carPos, hitParam.hitDir, radius1, radius1)

            // We want to minimize this error
            fun error(ala: ArcLineArc): Float {
                val timeSpentTurning1 = timeSpentTurning(curForwardSpeed, ala.angle1)
                val straight = DriveModel.drive1D(ala.straightLength, curForwardSpeed, car.boost.toFloat())
                val speedAtCirc2 = straight.endSpeed
                val deltaRadius = abs(abs(ala.radius2) - turnRadius(speedAtCirc2))
                val timeSpentTurning2 = timeSpentTurning(speedAtCirc2, ala.angle2)
                val time = timeSpentTurning1 + straight.timeSpent + timeSpentTurning2
                val hitImpulse = ((ala.endDir * speedAtCirc2) dot hitParam.hitDir) * hitParam.hitDir
                val hitImpulseDelta = (hitImpulse - hitParam.impulse).mag() // TODO Might have negative impact if speed is higher than desiredVel
                return deltaRadius + 10f * time + 0.5f * ala.length + hitImpulseDelta
            }

            // Convenience function to construct ArcLineArc from parameters
            fun parametrizedError(params: FloatArray): Float {
                val rot = Mat3.rotationMatrix(Vec3.UP, params[1])
                return error(ArcLineArc(car.pos.flat(), startDir.dir2D(), hitParam.carPos.flat(), rot dot (hitParam.hitDir.dir2D()), initALA.radius1.sign * radius1, initALA.radius2.sign * params[0]))
            }

            // Parameters for the ArcLineArc
            val curParams = floatArrayOf(
                radius1, // radius 2
                0f, // end angle offset
            )

            // Appropriate small numbers of each parameter
            val epsilon = floatArrayOf(5f, 0.001f)
            val alpha = floatArrayOf(4f, 0.003f)
            val momentum = floatArrayOf(0f, 0f)

            var ala = initALA

            // Gradient descent
            for (k in 0 until iterations) {
                val e = parametrizedError(curParams)

                if (e <= 1f) {
                    break
                } else {

                    // Find gradient per parameter
                    val gradient = floatArrayOf(0f, 0f)
                    for (i in 0 until 2) {
                        // Temporarily add small delta to parameter i to find how it affects the error
                        curParams[i] += epsilon[i]
                        gradient[i] = (parametrizedError(curParams) - e) / epsilon[i]
                        curParams[i] -= epsilon[i]
                    }

                    // Update parameters to minimize error
                    for (i in 0 until 2) {
                        momentum[i] = 0.8f * momentum[i] + gradient[i]
                        curParams[i] -= momentum[i] * alpha[i]
                    }

                    curParams[1] = curParams[1].coerceIn(-0.6f, 0.6f) // Angle can't be greater than PI/4
                }
            }
            val rot = Mat3.rotationMatrix(Vec3.UP, curParams[1])
            val result = ArcLineArc(car.pos, startDir.dir2D(), hitParam.carPos.flat(), rot dot (hitParam.hitDir.dir2D()), initALA.radius1.sign * radius1, initALA.radius2.sign * curParams[0])

            if (draw != null) {
                draw.arcLineArc(initALA, Color.RED)
                draw.arcLineArc(result, Color.RED)
            }

            return result
        }
    }
}