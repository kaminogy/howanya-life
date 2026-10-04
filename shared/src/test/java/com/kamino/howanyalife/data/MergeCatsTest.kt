package com.kamino.howanyalife.data

import com.kamino.howanyalife.model.CatState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeCatsTest {

    private val hour = 3_600_000L
    /** Real-time advance, independent of the debug build's time scale. */
    private val advance: (CatState, Long) -> CatState = { state, millis -> state.advance(millis) }

    private fun merge(a: SavedCat, b: SavedCat) = mergeCats(a, b, advance)

    @Test
    fun newerSnapshotWins() {
        val older = SavedCat(CatState(fullness = 50f, energy = 90f, mood = 50f), 0)
        val newer = SavedCat(CatState(fullness = 40f, energy = 20f, mood = 40f, asleep = true), hour)
        val merged = merge(older, newer)
        assertEquals(hour, merged.savedAtMillis)
        assertEquals(20f, merged.state.energy, 0f)
        assertTrue(merged.state.asleep)
    }

    @Test
    fun feedingOnTheOtherDeviceIsNotLostToANearlySimultaneousSave() {
        // The watch fed the cat; a moment later the phone autosaved without knowing.
        val fedOnWatch = SavedCat(CatState(fullness = 95f, mood = 80f), 1_000)
        val phoneAutosave = SavedCat(CatState(fullness = 50f, mood = 60f), 2_000)
        val merged = merge(fedOnWatch, phoneAutosave)
        assertEquals(2_000, merged.savedAtMillis)
        assertTrue(merged.state.fullness > 94f)
        assertTrue(merged.state.mood > 79f)
    }

    @Test
    fun orderDoesNotMatter() {
        val a = SavedCat(CatState(fullness = 90f, energy = 30f, mood = 70f), 0)
        val b = SavedCat(CatState(fullness = 60f, energy = 80f, mood = 75f), hour / 2)
        assertTrue(merge(a, b).sameAs(merge(b, a)))
    }

    @Test
    fun mergingAgainChangesNothing() {
        val a = SavedCat(CatState(fullness = 90f, energy = 30f, mood = 70f), 0)
        val b = SavedCat(CatState(fullness = 60f, energy = 80f, mood = 75f), hour / 2)
        val once = merge(a, b)
        assertTrue(merge(once, b).sameAs(once))
        assertTrue(merge(once, a).sameAs(once))
    }

    @Test
    fun aLongForgottenSnapshotDoesNotRefillTheCat() {
        // The other device was off for a day; its old full bowl has long been digested.
        val stale = SavedCat(CatState(fullness = 100f), 0)
        val current = SavedCat(CatState(fullness = 30f), 24 * hour)
        assertEquals(30f, merge(stale, current).state.fullness, 0.01f)
    }
}
