package east.rlbot.data

import east.rlbot.math.Mat3
import east.rlbot.math.Plane
import east.rlbot.math.Vec3
import east.rlbot.util.PIf
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

object Arena {
    const val LENGTH = 10240f
    const val LENGTH2 = LENGTH / 2f
    const val WIDTH = 8192f
    const val WIDTH2 = WIDTH / 2f
    const val HEIGHT = 2044f
    const val HEIGHT2 = HEIGHT / 2f
    val SEMI_SIZE = Vec3(WIDTH2, LENGTH2, HEIGHT / 2f)

    const val CORNER_WALL_AX_INTERSECT = 8064

    val GROUND = Plane(Vec3(), Vec3(z=1))
    val CEILING = Plane(Vec3(z= HEIGHT), Vec3(z=-1))
    val BLUE_BACKBOARD = Plane(Vec3(y=-LENGTH2), Vec3(y=1))
    val ORANGE_BACKBOARD = Plane(Vec3(y= LENGTH2), Vec3(y=-1))
    val LEFT_WALL = Plane(Vec3(x= WIDTH2), Vec3(x=-1)) // Blue POV
    val RIGHT_WALL = Plane(Vec3(x=-WIDTH2), Vec3(x=1)) // Blue POV
    val BLUE_RIGHT_CORNER_WALL = Plane(Vec3(y=-CORNER_WALL_AX_INTERSECT), Vec3(x=1, y=1).dir())
    val BLUE_LEFT_CORNER_WALL = Plane(Vec3(y=-CORNER_WALL_AX_INTERSECT), Vec3(x=-1, y=1).dir())
    val ORANGE_RIGHT_CORNER_WALL = Plane(Vec3(y=CORNER_WALL_AX_INTERSECT), Vec3(x=-1, y=-1).dir())
    val ORANGE_LEFT_CORNER_WALL = Plane(Vec3(y=CORNER_WALL_AX_INTERSECT), Vec3(x=1, y=-1).dir())

    val ALL_WALLS = listOf(
        GROUND,
        CEILING,
        BLUE_BACKBOARD,
        ORANGE_BACKBOARD,
        LEFT_WALL,
        RIGHT_WALL,
        BLUE_RIGHT_CORNER_WALL,
        BLUE_LEFT_CORNER_WALL,
        ORANGE_RIGHT_CORNER_WALL,
        ORANGE_LEFT_CORNER_WALL
    )

    val SIDE_WALLS = listOf(
        BLUE_BACKBOARD,
        ORANGE_BACKBOARD,
        LEFT_WALL,
        RIGHT_WALL,
        BLUE_RIGHT_CORNER_WALL,
        BLUE_LEFT_CORNER_WALL,
        ORANGE_RIGHT_CORNER_WALL,
        ORANGE_LEFT_CORNER_WALL
    )

    val SIDE_WALLS_AND_GROUND = listOf(
        GROUND,
        BLUE_BACKBOARD,
        ORANGE_BACKBOARD,
        LEFT_WALL,
        RIGHT_WALL,
        BLUE_RIGHT_CORNER_WALL,
        BLUE_LEFT_CORNER_WALL,
        ORANGE_RIGHT_CORNER_WALL,
        ORANGE_LEFT_CORNER_WALL
    )

    /**
     * An approximation of the arena geometry using signed distance fields. The approximation includes the base cube,
     * the subtracted corners, roundness, and the goals (without roundness).
     */
    object SDF {

        private const val ROUNDNESS = 300f

        private val ROT_45 = cos(Math.PI/4f)
        private val ROT_45_MAT = Mat3.rotationMatrix(Vec3.UP, PIf / 4f)
        private val CORNER_SEMI_SIZE = Vec3(ROT_45 * CORNER_WALL_AX_INTERSECT, ROT_45 * CORNER_WALL_AX_INTERSECT, HEIGHT2)

        private val GOALS_SEMI_SIZE = Vec3(Goal.WIDTH2, LENGTH2 + Goal.DEPTH, Goal.HEIGHT / 2f)

        /**
         * Returns the distance to the nearest wall of the SDF approximation. The result is negative if the point
         * is outside the arena.
         */
        fun wallDistance(point: Vec3): Float {
            // SDF box https://www.youtube.com/watch?v=62-pRVZuS5c
            // SDF rounded corners https://www.youtube.com/watch?v=s5NGeUV2EyU

            // Base cube
            val baseQ = (point - Vec3(z = HEIGHT2)).abs() - SEMI_SIZE + Vec3.ONES * ROUNDNESS
            val baseDistOutside = baseQ.coerceAtLeast(Vec3.ZERO).mag()
            val baseDistInside = min(baseQ.maxComponent(), 0f) // negative if point is inside
            val baseDist = baseDistOutside + baseDistInside

            // Corners cube
            val cornerQ = ((ROT_45_MAT dot point) - Vec3(z = HEIGHT2)).abs() - CORNER_SEMI_SIZE + Vec3.ONES * ROUNDNESS
            val cornerDistOutside = cornerQ.coerceAtLeast(Vec3.ZERO).mag()
            val cornerDistInside = min(cornerQ.maxComponent(), 0f) // negative if point is inside
            val cornerDist = cornerDistOutside + cornerDistInside

            // Intersection of base and corners
            val baseCornerDist = max(baseDist, cornerDist) - ROUNDNESS

            // Goals cube
            val goalsQ = (point - Vec3(z = Goal.HEIGHT / 2f)).abs() - GOALS_SEMI_SIZE
            val goalsDistOutside = goalsQ.coerceAtLeast(Vec3.ZERO).mag()
            val goalsDistInside = min(goalsQ.maxComponent(), 0f) // negative if point is inside
            val goalsDist = goalsDistOutside + goalsDistInside

            // Union with goals and invert result
            return -min(baseCornerDist, goalsDist)
        }

        /**
         * Returns the normalized gradient at the given point. At wall distance 0 this is the arena's surface normal.
         */
        fun normal(point: Vec3): Vec3 {
            // SDF normals https://www.iquilezles.org/www/articles/normalsSDF/normalsSDF.htm
            val h = 0.0004f
            return Vec3(
                wallDistance(point + Vec3(h, 0f, 0f)) - wallDistance(point - Vec3(h, 0f, 0f)),
                wallDistance(point + Vec3(0f, h, 0f)) - wallDistance(point - Vec3(0f, h, 0f)),
                wallDistance(point + Vec3(0f, 0f, h)) - wallDistance(point - Vec3(0f, 0f, h)),
            ).dir()
        }

        /**
         * Returns true if the given point is inside the SDF arena approximation
         */
        fun contains(point: Vec3): Boolean {
            return wallDistance(point) > 0f
        }
    }
}
