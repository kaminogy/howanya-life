package com.kamino.howanyalife.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatStateTest {

    private val hour = 3_600_000L

    @Test
    fun fullnessEmptiesAfterAboutEightHours() {
        val state = CatState(fullness = 100f).advance(8 * hour)
        assertEquals(0f, state.fullness, 0.01f)
    }

    @Test
    fun energyDropsAwakeAndRecoversAsleep() {
        val start = CatState(energy = 50f)
        assertTrue(start.advance(hour).energy < 50f)
        assertTrue(start.copy(asleep = true).advance(hour).energy > 50f)
    }

    @Test
    fun hungryCatGetsGrumpy() {
        val state = CatState(fullness = 10f, mood = 60f).advance(hour)
        assertTrue(state.mood < 60f)
    }

    @Test
    fun statsStayWithinBounds() {
        val longAway = CatState().advance(1_000 * hour)
        assertEquals(0f, longAway.fullness, 0f)
        assertEquals(0f, longAway.mood, 0f)

        val stuffed = CatState(fullness = 90f, mood = 95f).fed()
        assertEquals(100f, stuffed.fullness, 0f)
        assertEquals(100f, stuffed.mood, 0f)
    }

    @Test
    fun negativeElapsedTimeChangesNothing() {
        val state = CatState()
        assertEquals(state, state.advance(-hour))
    }
}
