package east.rlbot.simulation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * A lookup table with entries describing the relationship between time, distance, and speed during acceleration.
 */
class StraightAccelerationLUT(inputStream: InputStream) {

    private val entries: List<Entry> = csvReader().readAllWithHeader(inputStream).map { row ->
        Entry(
            row["time"]!!.toFloat(),
            row["distance"]!!.toFloat(),
            row["speed"]!!.toFloat(),
        )
    }

    /**
     * Simulate acceleration until the given limit is reached. Boost limit is equivalent to a time limit.
     */
    fun simUntilLimit(
        initialSpeed: Float,
        timeLimit: Float? = null,
        distanceLimit: Float? = null,
        maxSpeedLimit: Float? = null,
    ): LookupResult {
        // At least one limit expected
        assert(timeLimit != null || distanceLimit != null || maxSpeedLimit != null) { "No limit set" }

        // limits must be positive
        if (timeLimit != null) assert(timeLimit > 0)
        if (maxSpeedLimit != null) assert(maxSpeedLimit > 0)
        if (distanceLimit != null) assert(distanceLimit > 0)

        // Start entry from initial speed
        val startIndex = findIndex(initialSpeed) { it.speed }
        val startEntry = entries[startIndex]

        var timeLimitIndex = entries.size - 1
        var distanceLimitIndex = entries.size - 1
        var maxSpeedLimitIndex = entries.size - 1

        if (timeLimit != null)
            timeLimitIndex = findIndex(startEntry.time + timeLimit) { it.time }
        if (distanceLimit != null)
            distanceLimitIndex = findIndex(startEntry.distance + distanceLimit) { it.distance }
        if (maxSpeedLimit != null)
            maxSpeedLimitIndex = findIndex(maxSpeedLimit) { it.speed }

        // Find soonest reached limit
        val resultIndex = listOf(timeLimitIndex, distanceLimitIndex, maxSpeedLimitIndex).minOrNull()!!.coerceAtLeast(startIndex)
        val resultEntry = entries[resultIndex]

        return LookupResult(
            duration = resultEntry.time - startEntry.time,
            distance = resultEntry.distance - startEntry.distance,
            endSpeed = resultEntry.speed,
            durationLimitReached = timeLimit != null && resultIndex == timeLimitIndex,
            distanceLimitReached = distanceLimit != null && resultIndex == distanceLimitIndex,
            speedLimitReached = maxSpeedLimit != null && resultIndex == maxSpeedLimitIndex,
        )
    }

    /**
     * Returns the index of the entry where the given property reaches the given value, or the entry right before it,
     * if the exact value is not found
     */
    private fun findIndex(value: Float, property: (Entry) -> Float): Int {
        return entries.binarySearch { (property(it) - value).sign.toInt() }.absoluteValue.coerceAtMost(entries.size - 1)
    }

    data class Entry(
        val time: Float,
        val distance: Float,
        val speed: Float,
    )

    data class LookupResult(
        val duration: Float,
        val distance: Float,
        val endSpeed: Float,
        val durationLimitReached: Boolean,
        val distanceLimitReached: Boolean,
        val speedLimitReached: Boolean,
    )
}