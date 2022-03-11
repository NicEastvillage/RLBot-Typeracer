package east.rlbot.simulation

import east.rlbot.data.Car
import east.rlbot.math.Vec3
import east.rlbot.util.DebugDraw
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sign

class JumpLUT(private val entries: List<Float>, val dt: Float) {

    /**
     * Simulate jumping until the given limit is reached
     */
    fun simUntilLimit(
        timeLimit: Float? = null,
        heightLimit: Float? = null,
    ): LookupResult {
        // At least one limit expected
        assert(timeLimit != null || heightLimit != null) { "No limit set" }

        // limits must be positive
        if (timeLimit != null) assert(timeLimit >= 0)
        if (heightLimit != null) assert(heightLimit >= Car.REST_HEIGHT)

        var timeLimitIndex = entries.size - 1
        var heightLimitIndex = entries.size - 1

        if (timeLimit != null)
            timeLimitIndex = (timeLimit / dt).toInt().coerceAtMost(entries.size - 1)
        if (heightLimit != null)
            heightLimitIndex = findHeightIndex(heightLimit)

        // Find soonest reached limit
        val resultIndex = min(timeLimitIndex, heightLimitIndex).coerceAtLeast(0)
        val resultTime = resultIndex * dt
        val resultHeight = entries[resultIndex]

        return LookupResult(
            time = resultTime,
            height = resultHeight,
            timeLimitReached = timeLimit != null && timeLimitIndex == resultIndex,
            heightLimitReached = heightLimit != null && heightLimitIndex == resultIndex,
        )
    }

    /**
     * Returns the index of the entry where the height reaches the given value, or the entry right before it,
     * if the exact value is not found
     */
    private fun findHeightIndex(height: Float): Int {
        return entries.binarySearch { (it - height).sign.toInt() }.absoluteValue.coerceAtMost(entries.size - 1)
    }

    fun maxHeight() = entries.last()

    fun render(draw: DebugDraw) {
        draw.polyline(entries.chunked(6).map { it.first() }.withIndex().map { (i, height) -> Vec3(x=i*15f,z=height) }, Color.WHITE)
    }

    data class LookupResult(
        val time: Float,
        val height: Float,
        val timeLimitReached: Boolean,
        val heightLimitReached: Boolean,
    )
}