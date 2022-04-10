package east.rlbot.topology

import east.rlbot.data.BoostPad


fun BoostphobiaGraph.astarToPad(start: BoostphobiaGraph.Vertex, endPad: BoostPad): List<BoostphobiaGraph.Vertex>? {
    val end = vertices[endPad.index]
    val visited = mutableSetOf<BoostphobiaGraph.Vertex>()
    val cheapestKnownDist = mutableMapOf<BoostphobiaGraph.Vertex, Float>() // g
    val estimatedDist = mutableMapOf<BoostphobiaGraph.Vertex, Float>()  // f = g + h
    val cameFrom = mutableMapOf<BoostphobiaGraph.Vertex, BoostphobiaGraph.Vertex>()

    cheapestKnownDist[start] = 0f
    estimatedDist[start] = start.pos.dist(end.pos)
    val queue = mutableListOf(start)

    while (queue.isNotEmpty()) {
        val cur = queue.minByOrNull { estimatedDist[it]!! }!!
        if (cur == end) {
            // Reconstruct path
            var vert = cur
            val path = mutableListOf(vert)
            while (cameFrom[vert] != null) {
                vert = cameFrom[vert]!!
                path.add(0, vert)
            }
            return path
        }
        queue.remove(cur)
        visited.add(cur)
        for (nei in cur.neighbours) {
            val tentativeScore = cheapestKnownDist[cur]!! + cur.pos.dist(nei.pos) + (if (nei.pad == null) 0f else 10000f)
            if (tentativeScore < (cheapestKnownDist[nei] ?: Float.POSITIVE_INFINITY)) {
                // New cheapest found
                cameFrom[nei] = cur
                cheapestKnownDist[nei] = tentativeScore
                estimatedDist[nei] = tentativeScore + nei.pos.dist(end.pos)
                if (nei !in visited) {
                    queue.add(nei)
                }
            }
        }
    }

    return null
}