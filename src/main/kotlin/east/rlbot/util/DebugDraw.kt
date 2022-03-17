package east.rlbot.util

import east.rlbot.data.Car
import east.rlbot.math.*
import rlbot.cppinterop.RLBotDll
import rlbot.cppinterop.RLBotInterfaceException
import java.awt.Color
import java.awt.Point
import kotlin.math.*

class DebugDraw(
    val renderer: BasicRenderer,
) {
    constructor(botIndex: Int) : this(BasicRenderer(botIndex))

    var color: Color = Color.WHITE

    @Deprecated("Removed in Psyonix API")
    fun rect2D(x: Int, y: Int, width: Int, height: Int, fill: Boolean = true, color: Color = this.color) {
        renderer.drawRectangle2d(color, Point(x, y), width, height, fill)
    }

    fun string2D(x: Int, y: Int, text: String, scale: Int = 1, color: Color = this.color) {
        renderer.drawString2d(text, color, Point(x, y), scale, scale)
    }

    fun string3D(pos: Vec3, text: String, scale: Int = 1, color: Color = this.color) {
        renderer.drawString3d(text, color, pos, scale, scale)
    }

    fun rect3D(pos: Vec3, width: Int, height: Int, fill: Boolean = true, centered: Boolean = true, color: Color = this.color) {
        if (centered) renderer.drawCenteredRectangle3d(color, pos, width, height, fill)
        else renderer.drawRectangle3d(color, pos, width, height, fill)
    }

    fun line(start: Vec3, end: Vec3, color: Color = this.color) {
        renderer.drawLine3d(color, start, end)
    }

    fun polyline(positions: List<Vec3>, color: Color = this.color) {
        positions.zipWithNext { a, b ->
            line(a, b, color)
        }
    }

    fun orientedOrigin(center: Vec3, ori: Mat3, size: Float = 125f, forwardColor: Color = Color.RED, rightColor: Color = Color.GREEN, upColor: Color = Color.BLUE) {
        line(center, center + ori.forward * size, forwardColor)
        line(center, center + ori.right * size, rightColor)
        line(center, center + ori.up * size, upColor)
    }

    fun circle(center: Vec3, normal: Vec3, radius: Float, color: Color = this.color) {

        var arm = (normal cross center).dir() * radius
        val pieces = radius.pow(0.7f).toInt() + 5
        val angle = 2 * PIf / pieces
        val rotMat = Mat3.rotationMatrix(normal.dir(), angle)

        val points = mutableListOf(center + arm)
        for (i in 0 until pieces) {
            arm = rotMat dot arm
            points.add(center + arm)
        }

        polyline(points, color)
    }

    fun octagon(center: Vec3, radius: Float, color: Color = this.color) {
        val r = radius / 1.4142135
        polyline(listOf(
            center + Vec3(radius),
            center + Vec3(r, r),
            center + Vec3(0, radius),
            center + Vec3(-r, r),
            center + Vec3(-radius),
            center + Vec3(-r, -r),
            center + Vec3(0, -radius),
            center + Vec3(r, -r),
            center + Vec3(radius),
        ), color)
    }

    fun arc(center: Vec3, normal: Vec3, arm: Vec3, radians: Float, color: Color = this.color) {
        val frac = abs(radians).coerceIn(0f, 2f * PIf) / (2f * PIf)
        var arm = arm
        val pieces = ((arm.mag().pow(0.7f) + 5f) * frac).toInt()
        val angle = radians.coerceIn(-2f * PIf, 2f * PIf) / pieces
        val rotMat = Mat3.rotationMatrix(normal.dir(), angle)

        val points = mutableListOf(center + arm)
        for (i in 0 until pieces) {
            arm = rotMat dot arm
            points.add(center + arm)
        }

        polyline(points, color)
    }

    fun cross(pos: Vec3, size: Float, color: Color = this.color) {
        line(pos + Vec3(x=size), pos + Vec3(x=-size), color)
        line(pos + Vec3(y=size), pos + Vec3(y=-size), color)
        line(pos + Vec3(z=size), pos + Vec3(z=-size), color)
    }

    fun crossAngled(pos: Vec3, size: Float, color: Color = this.color) {
        val r = size / 1.4142135
        line(pos + Vec3(r, r, r), pos + Vec3(-r, -r, -r), color)
        line(pos + Vec3(r, r, -r), pos + Vec3(-r, -r, r), color)
        line(pos + Vec3(r, -r, -r), pos + Vec3(-r, r, r), color)
        line(pos + Vec3(r, -r, r), pos + Vec3(-r, r, -r), color)
    }

    fun cube(center: Vec3, size: Float, color: Color = this.color) {
        cube(center, Vec3(size, size, size), color)
    }

    fun cube(center: Vec3, size: Vec3, color: Color = this.color) {
        val xr = size.x / 2f
        val yr = size.y / 2f
        val zr = size.z / 2f

        line(center + Vec3(-xr, -yr, -zr), center + Vec3(-xr, -yr, zr), color)
        line(center + Vec3(xr, -yr, -zr), center + Vec3(xr, -yr, zr), color)
        line(center + Vec3(-xr, yr, -zr), center + Vec3(-xr, yr, zr), color)
        line(center + Vec3(xr, yr, -zr), center + Vec3(xr, yr, zr), color)

        line(center + Vec3(-xr, -yr, -zr), center + Vec3(-xr, yr, -zr), color)
        line(center + Vec3(xr, -yr, -zr), center + Vec3(xr, yr, -zr), color)
        line(center + Vec3(-xr, -yr, zr), center + Vec3(-xr, yr, zr), color)
        line(center + Vec3(xr, -yr, zr), center + Vec3(xr, yr, zr), color)

        line(center + Vec3(-xr, -yr, -zr), center + Vec3(xr, -yr, -zr), color)
        line(center + Vec3(-xr, -yr, zr), center + Vec3(xr, -yr, zr), color)
        line(center + Vec3(-xr, yr, -zr), center + Vec3(xr, yr, -zr), color)
        line(center + Vec3(-xr, yr, zr), center + Vec3(xr, yr, zr), color)
    }

    fun orientedCube(center: Vec3, oriCube: OrientedCube, color: Color = this.color) {
        val ori = oriCube.ori
        val xr = oriCube.size.x / 2f
        val yr = oriCube.size.y / 2f
        val zr = oriCube.size.z / 2f

        line(center + ori.toGlobal(Vec3(-xr, -yr, -zr)), center + ori.toGlobal(Vec3(-xr, -yr, zr)), color)
        line(center + ori.toGlobal(Vec3(xr, -yr, -zr)), center + ori.toGlobal(Vec3(xr, -yr, zr)), color)
        line(center + ori.toGlobal(Vec3(-xr, yr, -zr)), center + ori.toGlobal(Vec3(-xr, yr, zr)), color)
        line(center + ori.toGlobal(Vec3(xr, yr, -zr)), center + ori.toGlobal(Vec3(xr, yr, zr)), color)

        line(center + ori.toGlobal(Vec3(-xr, -yr, -zr)), center + ori.toGlobal(Vec3(-xr, yr, -zr)), color)
        line(center + ori.toGlobal(Vec3(xr, -yr, -zr)), center + ori.toGlobal(Vec3(xr, yr, -zr)), color)
        line(center + ori.toGlobal(Vec3(-xr, -yr, zr)), center + ori.toGlobal(Vec3(-xr, yr, zr)), color)
        line(center + ori.toGlobal(Vec3(xr, -yr, zr)), center + ori.toGlobal(Vec3(xr, yr, zr)), color)

        line(center + ori.toGlobal(Vec3(-xr, -yr, -zr)), center + ori.toGlobal(Vec3(xr, -yr, -zr)), color)
        line(center + ori.toGlobal(Vec3(-xr, -yr, zr)), center + ori.toGlobal(Vec3(xr, -yr, zr)), color)
        line(center + ori.toGlobal(Vec3(-xr, yr, -zr)), center + ori.toGlobal(Vec3(xr, yr, -zr)), color)
        line(center + ori.toGlobal(Vec3(-xr, yr, zr)), center + ori.toGlobal(Vec3(xr, yr, zr)), color)
    }

    fun octahedron(center: Vec3, size: Float, color: Color = this.color) {
        val r = size / 2f

        line(center + Vec3(r, 0, 0), center + Vec3(0, r, 0), color)
        line(center + Vec3(0, r, 0), center + Vec3(-r, 0, 0), color)
        line(center + Vec3(-r, 0, 0), center + Vec3(0, -r, 0), color)
        line(center + Vec3(0, -r, 0), center + Vec3(r, 0, 0), color)

        line(center + Vec3(r, 0, 0), center + Vec3(0, 0, r), color)
        line(center + Vec3(0, 0, r), center + Vec3(-r, 0, 0), color)
        line(center + Vec3(-r, 0, 0), center + Vec3(0, 0, -r), color)
        line(center + Vec3(0, 0, -r), center + Vec3(r, 0, 0), color)

        line(center + Vec3(0, r, 0), center + Vec3(0, 0, r), color)
        line(center + Vec3(0, 0, r), center + Vec3(0, -r, 0), color)
        line(center + Vec3(0, -r, 0), center + Vec3(0, 0, -r), color)
        line(center + Vec3(0, 0, -r), center + Vec3(0, r, 0), color)
    }

    fun arcLineArc(ala: ArcLineArc, color: Color = this.color, secColor: Color = color.half()) {
        ala.run {
            arc(circle1Center.withZ(Car.REST_HEIGHT), Vec3.UP, tangentPoint1 - circle1Center, -angle1 * radius1.sign)
            line(circle1Center.withZ(Car.REST_HEIGHT), start.withZ(Car.REST_HEIGHT), secColor)
            line(circle1Center.withZ(Car.REST_HEIGHT), tangentPoint1.withZ(Car.REST_HEIGHT), secColor)

            line(tangentPoint1.withZ(Car.REST_HEIGHT), tangentPoint2.withZ(Car.REST_HEIGHT))

            arc(circle2Center.withZ(Car.REST_HEIGHT), Vec3.UP, tangentPoint2 - circle2Center, angle2 * radius2.sign)
            line(circle2Center.withZ(Car.REST_HEIGHT), tangentPoint2.withZ(Car.REST_HEIGHT), secColor)
            line(circle2Center.withZ(Car.REST_HEIGHT), end.withZ(Car.REST_HEIGHT), secColor)
        }
    }

    fun aimCone(pos: Vec3, cone: AimCone, size: Float = 120f, color: Color = this.color) {
        line(pos, pos + size * cone.centerDir, color)
        line(pos, pos + size * cone.clamp(cone.centerDir.perp2D()), color)
        line(pos, pos + size * cone.clamp(cone.centerDir.perp2D().perp2D().perp2D()), color)
        circle(pos + cos(cone.angle) * size * cone.centerDir, cone.centerDir, sin(cone.angle) * size, color)
    }

    fun ballTrajectory(duration: Float, color: Color = this.color) {
        try {
            val ballPrediction = RLBotDll.getBallPrediction()
            var prev: Vec3? = null
            val stop = (60 * clamp(duration, 0f, 6f)).toInt()
            var i = 0
            while (i < ballPrediction.slicesLength()) {
                val slice = ballPrediction.slices(i)
                if (i >= stop) {
                    break
                }
                val location = Vec3(slice.physics().location())
                prev?.let { line(it, location, color) }
                prev = location
                i += 4
            }
        } catch (ignored: RLBotInterfaceException) {
        }
    }

    fun start() {
        renderer.startPacket()
    }

    fun send() {
        renderer.finishAndSendIfDifferent()
    }
}