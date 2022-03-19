package east.rlbot.topology

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import east.rlbot.data.BoostPad
import east.rlbot.data.BoostPadManager
import east.rlbot.math.Vec3
import east.rlbot.util.DebugDraw
import java.awt.Color

class BoostphobiaGraph(
    val vertices: List<Vertex>,
    val edges: List<Pair<Int, Int>>,
){
    class Vertex(
        val id: Int,
        val pos: Vec3,
        val pad: BoostPad?,
        val neighbours: MutableList<Vertex>,
    )

    fun render(draw: DebugDraw) {
        draw.color = Color.ORANGE
        for (v in vertices) {
            if (v.pad == null) draw.color = Color.CYAN // Vertices with pads are always first. Switch color when we find one without pad
            //draw.octagon(v.pos.withZ(20), 150f)
            draw.string3D(v.pos.withZ(30), v.id.toString())
        }
        draw.color = Color.GRAY
        for ((v1, v2) in edges) {
            draw.line(vertices[v1].pos.withZ(20), vertices[v2].pos.withZ(20))
        }
    }
    companion object {
        fun load() = loadBoostphobiaGraph()
    }
}

private fun loadBoostphobiaGraph(): BoostphobiaGraph {

    val padVertices = BoostPadManager.allPads.mapIndexed { i, pad ->
        BoostphobiaGraph.Vertex(i, pad.pos, pad, mutableListOf())
    }

    var i = padVertices.size
    val vertices = padVertices + with(ClassLoader.getSystemResourceAsStream("vertices.csv")) {
        csvReader().readAllWithHeader(this!!).map { row ->
            try {
                BoostphobiaGraph.Vertex(
                    i++,
                    Vec3(row["x"]!!.toFloat(), row["y"]!!.toFloat()),
                    null,
                    mutableListOf()
                )
            } catch (e: Exception) {
                println("Vertex error!! $row")
                BoostphobiaGraph.Vertex(i - 1, Vec3(), null, mutableListOf())
            }
        }
    }

    val edges = with(ClassLoader.getSystemResourceAsStream("edges.csv")) {
        csvReader().readAllWithHeader(this!!).map { row ->
            try {
                row["start"]!!.toInt() to row["end"]!!.toInt()
            }
            catch (e: Exception) {
                println("Edge error!! $row")
                0 to 0
            }
        }
    }

    for ((v1, v2) in edges) {
        vertices[v1].neighbours.add(vertices[v2])
        vertices[v2].neighbours.add(vertices[v1])
    }

    for (vertex in vertices) {
        vertex.neighbours.sortBy { it.id }
    }

    return BoostphobiaGraph(vertices, edges)
}