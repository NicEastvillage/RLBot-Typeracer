package east.rlbot.simulation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import east.rlbot.math.Mat3
import east.rlbot.math.Vec3
import java.io.InputStream
import kotlin.math.sign

class TurningDecelerationLUT(inputStream: InputStream) {

    val entries: List<Entry> = csvReader().readAllWithHeader(inputStream).map { row ->
        Entry(
            row["time"]!!.toFloat(),
            row["angle"]!!.toFloat(),
            row["speed"]!!.toFloat(),
            Vec3(row["x"]!!.toFloat(), row["y"]!!.toFloat()),
        )
    }

    fun simUntilLimit(
        initialSpeed: Float,
        timeLimit: Float? = null,
        angleLimit: Float? = null,
        minSpeedLimit: Float? = null,
    ): LookupResult {
        // At least one limit expected
        assert(timeLimit != null || angleLimit != null || minSpeedLimit != null) { "No limit set" }

        // Limits must be positive
        if (timeLimit != null) assert(timeLimit > 0)
        if (minSpeedLimit != null) assert(minSpeedLimit >= 0)
        if (angleLimit != null) assert(angleLimit > 0)

        // Start entry from initial speed
        val startIndex = findIndex(initialSpeed) { it.speed }
        val startEntry = entries[startIndex]

        var timeLimitIndex = entries.size - 1
        var angleLimitIndex = entries.size - 1
        var minSpeedLimitIndex = entries.size - 1

        if (timeLimit != null)
            timeLimitIndex = findIndex(startEntry.time + timeLimit) { it.time }
        if (angleLimit != null)
            angleLimitIndex = findIndex(startEntry.angle + angleLimit) { it.angle }
        if (minSpeedLimit != null)
            minSpeedLimitIndex = findSpeedIndex(minSpeedLimit)

        // Find soonest reached limit
        val resultIndex = listOf(timeLimitIndex, angleLimitIndex, minSpeedLimitIndex).minOrNull()!!.coerceAtLeast(startIndex)
        val resultEntry = entries[resultIndex]

        // Find displacement relative to start position and angle
        val displacement = startEntry.localDisplacement - resultEntry.localDisplacement
        val startOri = Mat3.eulerToRotation(0f, startEntry.angle, 0f)
        val localDisplacement = startOri.toLocal(displacement)

        return LookupResult(
            duration = resultEntry.time - startEntry.time,
            angle = resultEntry.angle - startEntry.angle,
            endSpeed = resultEntry.speed,
            localDisplacement = localDisplacement,
            durationLimitReached = timeLimit != null && resultIndex == timeLimitIndex,
            angleLimitReached = angleLimit != null && resultIndex == angleLimitIndex,
            speedLimitReached = minSpeedLimit != null && resultIndex == minSpeedLimitIndex,
        )
    }

    /**
     * Returns the index of the entry where the given property reaches the given value, or the entry right before it,
     * if the exact value is not found. If the property is speed, use [findSpeedIndex] instead.
     */
    private fun findIndex(value: Float, property: (Entry) -> Float): Int {
        return entries.binarySearch { (property(it) - value).sign.toInt() }.coerceIn(0, entries.size - 1)
    }

    /**
     * Returns the index of the entry where the speed reaches the given value, or the entry right before it,
     * if the exact value is not found. Speed is descending, which is why it handled differently that other properties.
     */
    private fun findSpeedIndex(value: Float): Int {
        return entries.binarySearch { - (it.speed - value).sign.toInt() }
    }

    data class Entry(
        val time: Float,
        val angle: Float,
        val speed: Float,
        val localDisplacement: Vec3,
    )

    data class LookupResult(
        val duration: Float,
        val angle: Float,
        val endSpeed: Float,
        val localDisplacement: Vec3,
        val durationLimitReached: Boolean,
        val angleLimitReached: Boolean,
        val speedLimitReached: Boolean,
    )
}