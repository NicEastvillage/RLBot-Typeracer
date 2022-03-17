package east.rlbot.topology

import east.rlbot.math.LineSegment
import east.rlbot.math.Vec3

class Triangulation(a: Vec3, b: Vec3, c: Vec3) {

    val vertices = mutableListOf<Vertex>()
    var startingEdge: Edge

    init {
        val ea = makeEdge()
        val eb = makeEdge()
        val ec = makeEdge()
        startingEdge = ea

        splice(ea.sym(), eb)
        splice(eb.sym(), ec)
        splice(ec.sym(), ea)

        val va = Vertex(0, a, ea).also { vertices.add(it) }
        val vb = Vertex(1, b, eb).also { vertices.add(it) }
        val vc = Vertex(2, c, ec).also { vertices.add(it) }

        ea.setEndPoints(va, vb)
        eb.setEndPoints(vb, vc)
        ec.setEndPoints(vc, va)
    }

    fun insert(p: Vec3) {
        var e = locate(p)
        if (p == e.orig().pos || p == e.dest().pos) return
        if (onEdge(p, e)) {
            e = e.origPrev()
            deleteEdge(e.origNext())
        }

        var base = makeEdge()
        val vert = Vertex(vertices.size, p, base).also { vertices.add(it) }
        base.setEndPoints(e.orig(), vert)
        splice(base, e)
        startingEdge = base
        do {
            base = connect(e, base.sym())
            e = base.origPrev()
        } while (e.leftNext() != startingEdge)

        do {
            val t = e.origPrev()
            if (rightOf(t.dest().pos, e) && inCircle(e.orig().pos, t.dest().pos, e.dest().pos, p)) {
                swap(e)
                e = e.origPrev()
            } else if (e.origNext() == startingEdge) return
            e = e.origNext().leftPrev()
        } while (true)
    }

    fun makeEdge(): Edge {
        return QuadEdge().edges[0]
    }

    fun deleteEdge(e: Edge) {
        splice(e, e.origPrev())
        splice(e.sym(), e.sym().origPrev())
        // delete e.quad
    }

    fun splice(a: Edge, b: Edge) {
        val alpha = a.origNext().rot()
        val beta = b.origNext().rot()

        val t1 = b.origNext()
        val t2 = a.origNext()
        val t3 = beta.origNext()
        val t4 = alpha.origNext()

        a.next = t1
        b.next = t2
        alpha.next = t3
        beta.next = t4
    }

    fun connect(a: Edge, b: Edge): Edge {
        val e = makeEdge()
        splice(e, a.leftNext())
        splice(e.sym(), b)
        e.setEndPoints(a.dest(), b.orig())
        return e
    }

    fun swap(e: Edge) {
        val a = e.origPrev()
        val b = e.sym().origPrev()
        splice(e, a)
        splice(e.sym(), b)
        splice(e, a.leftNext())
        splice(e.sym(), b.leftNext())
        e.setEndPoints(a.dest(), b.dest())
    }

    fun triArea(a: Vec3, b: Vec3, c: Vec3) = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    fun inCircle(a: Vec3, b: Vec3, c: Vec3, d: Vec3): Boolean {
        return (a.x * a.x + a.y * a.y) * triArea(b, c, d) -
                (b.x * b.x + b.y * b.y) * triArea(a, c, d) +
                (c.x * c.x + c.y * c.y) * triArea(a, b, d) -
                (d.x * d.x + d.y * d.y) * triArea(a, b, c) > 0
    }

    fun ccw(a: Vec3, b: Vec3, c: Vec3) = triArea(a, b, c) > 0

    fun rightOf(p: Vec3, e: Edge) = ccw(p, e.dest().pos, e.orig().pos)

    fun leftOf(p: Vec3, e: Edge) = ccw(p, e.orig().pos, e.dest().pos)

    fun onEdge(p: Vec3, e: Edge) = p.dist(LineSegment(e.orig().pos, e.dest().pos)) < 0.001f

    fun locate(p: Vec3): Edge {
        var e = startingEdge
        while (true) {
            e = when {
                p == e.orig().pos || p == e.dest().pos -> return e
                rightOf(p, e) -> e.sym()
                !rightOf(p, e.origNext()) -> e.origNext()
                !rightOf(p, e.destPrev()) -> e.destPrev()
                else -> return e
            }
        }
    }

    class Vertex(
        val index: Int,
        val pos: Vec3,
        var edge: Edge,
    )

    class QuadEdge {
        val edges = arrayOf(
            Edge(this, 0),
            Edge(this, 1),
            Edge(this, 2),
            Edge(this, 3),
        )

        init {
            edges[0].next = edges[0]
            edges[1].next = edges[3]
            edges[2].next = edges[2]
            edges[3].next = edges[1]
        }
    }

    class Edge(
        val quad: QuadEdge,
        /** Relative index of this in its quad-edge */
        val num: Int,
    ) {
        /** The index of the edge that is CCW next this edge on its origin */
        lateinit var next: Edge

        /** The vertex at the origin */
        lateinit var vert: Vertex

        fun rot() = quad.edges[if (num < 3) num + 1 else num - 3]
        fun invRot() = quad.edges[if (num > 0) num - 1 else num + 3]
        fun sym() = quad.edges[if (num < 2) num + 2 else num - 2]

        fun origNext() = next
        fun origPrev() = rot().origNext().rot()
        fun destNext() = sym().origNext().sym()
        fun destPrev() = invRot().origNext().invRot()
        fun leftNext() = invRot().origNext().rot()
        fun leftPrev() = origNext().sym()
        fun rightNext() = rot().origNext().invRot()
        fun rightPrev() = sym().origNext()

        fun orig() = vert
        fun dest() = sym().vert

        fun setEndPoints(orig: Vertex, dest: Vertex) {
            vert = orig
            orig.edge = this
            sym().vert = dest
            dest.edge = sym()
        }
    }
}