package east.rlbot.topology

import east.rlbot.math.Vec3
import east.rlbot.util.DebugDraw
import java.awt.Color

class TriangulationManager(
    val points: List<Vec3>
) {
    val triangulation = Triangulation(
        Vec3(-9_000f, -15_000f, 25f),
        Vec3(-9_000f, 15_000f, 25f),
        Vec3(15_000f, 0f, 25f)
    )
    var nextToInsert = 0

    fun insertNext(): Boolean {
        if (nextToInsert < points.size) {
            triangulation.insert(points[nextToInsert] * Vec3(x=-1, 1, 1))
            nextToInsert++
            return true
        }
        return false
    }

    fun finish(d: DebugDraw) {
        draw(d)
        d.send()
        while (true) {
            if (!insertNext()) break
            d.start()
            draw(d)
            d.send()
        }
        d.start()
    }

    fun draw(draw: DebugDraw) {
        draw.color = Color.PINK
        for (vert in triangulation.vertices) {
            val pos = vert.pos * Vec3(x=-1, 1, 1)
            draw.string3D(pos + Vec3.UP * 60, vert.index.toString(), scale = 3)
            val startEdge = vert.edge
            var currentEdge = startEdge
            do {
                draw.line(pos, currentEdge.dest().pos * Vec3(x=-1, 1, 1))
                currentEdge = currentEdge.origNext()
            } while (currentEdge != startEdge)
        }
    }
}