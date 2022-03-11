package east.rlbot.simulation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import east.rlbot.math.Mat3
import east.rlbot.math.Vec3
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign

class TurnTransitionLUT(lutStream: InputStream, subLutStreams: List<InputStream>) {

    private val mainLut = csvReader().readAllWithHeader(lutStream).map { row ->
        MainLutEntry(row["graph index"]!!.toInt(), row["angular speed index"]!!.toInt())
    }.chunked(subLutStreams.size)

    private val luts = subLutStreams.map { TurnTransitionSubLUT(it) }

    private data class MainLutEntry(val lutIndex: Int, val angularSpeedIndex: Int)

    data class LookupResult(
        val duration: Float,
        val angle: Float,
        val endSpeed: Float,
        val endAngularSpeed: Float,
        val localDisplacement: Vec3,
        val durationLimitReached: Boolean,
        val angleLimitReached: Boolean,
    )

    fun simUntilLimit(
        initialSpeed: Float,
        initialAngularSpeed: Float,
        timeLimit: Float? = null,
        angleLimit: Float? = null,
    ) : LookupResult {
        val speedIndex = max(0, (initialSpeed.toInt() - LOWEST_SPEED) / SPEED_GRANULARITY)
        val angularSpeedIndex = (abs(initialAngularSpeed) / ANG_SPEED_GRANULARITY).toInt()
        val (lutIndex, startIndex) = mainLut[speedIndex][angularSpeedIndex]
        return luts[lutIndex].simUntilLimit(startIndex, timeLimit = timeLimit, angleLimit = angleLimit)
    }

    companion object {
        private const val LOWEST_SPEED = 150
        private const val SPEED_GRANULARITY = 50
        private const val LUTS = 1 + (2300 - LOWEST_SPEED) / SPEED_GRANULARITY
        private const val ANG_SPEED_GRANULARITY = 0.1f
    }

    private class TurnTransitionSubLUT(inputStream: InputStream) {

        val entries: List<Entry> = csvReader().readAllWithHeader(inputStream).map { row ->
            Entry(
                row["time"]!!.toFloat(),
                row["angle"]!!.toFloat(),
                row["speed"]!!.toFloat(),
                row["angular speed"]!!.toFloat(),
                Vec3(row["x"]!!.toFloat(), row["y"]!!.toFloat()),
            )
        }

        fun simUntilLimit(
            startIndex: Int,
            timeLimit: Float? = null,
            angleLimit: Float? = null,
        ): LookupResult {
            // At least one limit expected
            assert(timeLimit != null || angleLimit != null) { "No limit set" }

            // Limits must be positive
            if (timeLimit != null) assert(timeLimit > 0)
            if (angleLimit != null) assert(angleLimit > 0)

            // Start entry from initial angular speed
            val startEntry = entries[startIndex]

            var timeLimitIndex = entries.size - 1
            var angleLimitIndex = entries.size - 1

            if (timeLimit != null)
                timeLimitIndex = findIndex(startEntry.time + timeLimit) { it.time }
            if (angleLimit != null)
                angleLimitIndex = findIndex(startEntry.angle + angleLimit) { it.angle }

            // Find soonest reached limit
            val resultIndex = listOf(timeLimitIndex, angleLimitIndex).minOrNull()!!.coerceAtLeast(startIndex)
            val resultEntry = entries[resultIndex]

            // Find displacement relative to start position and angle
            val displacement = resultEntry.localDisplacement - startEntry.localDisplacement
            val startOri = Mat3.eulerToRotation(0f, startEntry.angle, 0f)
            val localDisplacement = startOri.toLocal(displacement)

            return LookupResult(
                duration = resultEntry.time - startEntry.time,
                angle = resultEntry.angle - startEntry.angle,
                endSpeed = resultEntry.speed,
                endAngularSpeed = resultEntry.angularSpeed,
                localDisplacement = localDisplacement,
                durationLimitReached = timeLimit != null && resultIndex == timeLimitIndex,
                angleLimitReached = angleLimit != null && resultIndex == angleLimitIndex,
            )
        }

        /**
         * Returns the index of the entry where the given property reaches the given value, or the entry right before it,
         * if the exact value is not found
         */
        private fun findIndex(value: Float, property: (Entry) -> Float): Int {
            return entries.binarySearch { (property(it) - value).sign.toInt() }.absoluteValue.coerceIn(0, entries.size - 1)
        }

        data class Entry(
            val time: Float,
            val angle: Float,
            val speed: Float,
            val angularSpeed: Float,
            val localDisplacement: Vec3,
        )
    }
}