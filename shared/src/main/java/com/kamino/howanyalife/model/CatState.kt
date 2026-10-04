package com.kamino.howanyalife.model

/**
 * The cat's needs, each in the range 0..100.
 *
 * [advance] is a pure function so time that passed while the app was closed can be
 * applied in one step on the next launch.
 */
data class CatState(
    val fullness: Float = 70f,
    val energy: Float = 80f,
    val mood: Float = 60f,
    val asleep: Boolean = false,
) {
    val isHungry: Boolean get() = fullness < HUNGRY_THRESHOLD
    val isTired: Boolean get() = energy < TIRED_THRESHOLD

    fun advance(elapsedMillis: Long): CatState {
        if (elapsedMillis <= 0) return this
        val hours = elapsedMillis / MILLIS_PER_HOUR

        val newFullness = fullness - FULLNESS_DROP_PER_HOUR * hours
        val newEnergy = if (asleep) {
            energy + ENERGY_GAIN_ASLEEP_PER_HOUR * hours
        } else {
            energy - ENERGY_DROP_AWAKE_PER_HOUR * hours
        }
        val moodDelta = when {
            newFullness < HUNGRY_THRESHOLD -> -MOOD_DROP_HUNGRY_PER_HOUR
            newFullness > 60f && newEnergy > 40f -> MOOD_GAIN_CONTENT_PER_HOUR
            else -> -MOOD_DRIFT_PER_HOUR
        }
        return copy(
            fullness = newFullness.clampStat(),
            energy = newEnergy.clampStat(),
            mood = (mood + moodDelta * hours).clampStat(),
        )
    }

    fun fed(): CatState = copy(
        fullness = (fullness + FEED_FULLNESS).clampStat(),
        mood = (mood + FEED_MOOD).clampStat(),
    )

    fun petted(): CatState = copy(mood = (mood + PET_MOOD).clampStat())

    companion object {
        const val HUNGRY_THRESHOLD = 25f
        const val TIRED_THRESHOLD = 20f

        private const val MILLIS_PER_HOUR = 3_600_000.0
        private const val FULLNESS_DROP_PER_HOUR = 100.0 / 8   // empty after ~8h
        private const val ENERGY_DROP_AWAKE_PER_HOUR = 100.0 / 12
        private const val ENERGY_GAIN_ASLEEP_PER_HOUR = 100.0 / 3
        private const val MOOD_DROP_HUNGRY_PER_HOUR = 100.0 / 3
        private const val MOOD_GAIN_CONTENT_PER_HOUR = 100.0 / 6
        private const val MOOD_DRIFT_PER_HOUR = 100.0 / 24

        private const val FEED_FULLNESS = 45f
        private const val FEED_MOOD = 15f
        private const val PET_MOOD = 5f

        private fun Double.clampStat(): Float = coerceIn(0.0, 100.0).toFloat()
        private fun Float.clampStat(): Float = coerceIn(0f, 100f)
    }
}
