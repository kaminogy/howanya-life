package com.kamino.howanyalife.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BehaviorEngineTest {

    private val noon = 12
    private val midnight = 0

    private fun sample(state: CatState, current: CatAction, hour: Int, n: Int = 2_000): Map<CatAction, Int> {
        val engine = BehaviorEngine(Random(42))
        return (1..n).map { engine.next(state, current, hour) }.groupingBy { it }.eachCount()
    }

    @Test
    fun tiredCatSleeps() {
        val engine = BehaviorEngine(Random(1))
        repeat(100) {
            assertEquals(CatAction.SLEEP, engine.next(CatState(energy = 10f), CatAction.WALK, noon))
        }
    }

    @Test
    fun sleepingCatStretchesWhenRested() {
        val engine = BehaviorEngine(Random(1))
        assertEquals(CatAction.STRETCH, engine.next(CatState(energy = 100f), CatAction.SLEEP, midnight))
    }

    @Test
    fun sleepingCatKeepsSleepingAtNightUntilRested() {
        val counts = sample(CatState(energy = 70f), CatAction.SLEEP, midnight)
        assertEquals(setOf(CatAction.SLEEP), counts.keys)
    }

    @Test
    fun hungryCatMostlyCries() {
        val counts = sample(CatState(fullness = 10f, energy = 80f), CatAction.IDLE, noon)
        val cries = counts[CatAction.CRY] ?: 0
        assertTrue("cried $cries times", cries > 1_000)
    }

    @Test
    fun happyCatRunsAndLaughsMoreThanGrumpyCat() {
        fun playful(mood: Float): Int {
            val counts = sample(CatState(fullness = 80f, energy = 80f, mood = mood), CatAction.IDLE, noon)
            return (counts[CatAction.RUN] ?: 0) + (counts[CatAction.LAUGH] ?: 0)
        }
        assertTrue(playful(100f) > playful(10f))
    }

    @Test
    fun nightMakesSleepLikelier() {
        val state = CatState(energy = 50f)
        val day = sample(state, CatAction.IDLE, noon)[CatAction.SLEEP] ?: 0
        val night = sample(state, CatAction.IDLE, midnight)[CatAction.SLEEP] ?: 0
        assertTrue(night > day)
    }

    @Test
    fun durationIsWithinActionRange() {
        val engine = BehaviorEngine(Random(7))
        CatAction.entries.forEach { action ->
            repeat(50) {
                val ms = engine.durationMillis(action)
                assertTrue(ms >= (action.minSeconds * 1000).toLong() && ms <= (action.maxSeconds * 1000).toLong())
            }
        }
    }
}
