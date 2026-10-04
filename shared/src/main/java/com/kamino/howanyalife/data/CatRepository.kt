package com.kamino.howanyalife.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kamino.howanyalife.model.CatState
import com.kamino.howanyalife.shared.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.catDataStore by preferencesDataStore(name = "cat")

/** Converts real elapsed time into cat time (debug builds run faster; see shared/build.gradle.kts). */
fun catTime(elapsedMillis: Long): Long = (elapsedMillis.coerceAtLeast(0) * BuildConfig.TIME_SCALE).toLong()

/** A saved [CatState] plus the wall-clock time it was saved at. */
data class SavedCat(val state: CatState, val savedAtMillis: Long)

/**
 * The cat's saved state on this device. Every save is also shared with the paired phone or
 * watch through [CatSync], and states from the other device come back in through [mergeRemote].
 */
class CatRepository(private val context: Context) {

    private val sync by lazy { CatSync(context) }

    suspend fun load(): SavedCat? = context.catDataStore.data.first().toSavedCat()

    /** Emits the saved state every time it changes, e.g. so the widget redraws after feeding. */
    fun observe(): Flow<SavedCat?> = context.catDataStore.data.map { it.toSavedCat() }

    /** Pulls in whatever the other device last shared (e.g. on app start). */
    suspend fun pullRemote() {
        sync.fetchAll().forEach { mergeRemote(it) }
    }

    /**
     * Merges a snapshot from the other device into the local state. Returns true if the local
     * state changed. If the merge kept something the other device doesn't have yet (a feeding
     * here, for example), the result is shared back so both sides end up the same.
     */
    suspend fun mergeRemote(remote: SavedCat): Boolean {
        var before: SavedCat? = null
        var merged: SavedCat? = null
        context.catDataStore.edit { prefs ->
            before = prefs.toSavedCat()
            val result = before?.let { mergeCats(it, remote) } ?: remote
            merged = result
            if (!result.sameAs(before)) prefs.write(result)
        }
        val result = merged ?: return false
        if (!result.sameAs(remote)) sync.publish(result, urgent = true)
        return !result.sameAs(before)
    }

    private fun Preferences.toSavedCat(): SavedCat? {
        val savedAt = this[SAVED_AT] ?: return null
        val state = CatState(
            fullness = this[FULLNESS] ?: return null,
            energy = this[ENERGY] ?: return null,
            mood = this[MOOD] ?: return null,
            asleep = this[ASLEEP] ?: false,
        )
        return SavedCat(state, savedAt)
    }

    /** The saved state with the time since it was saved applied, or null if nothing is saved yet. */
    suspend fun loadCurrent(nowMillis: Long = System.currentTimeMillis()): CatState? =
        load()?.let { it.state.advance(catTime(nowMillis - it.savedAtMillis)) }

    /** Feeds the cat without the app open (from the home-screen widget) and saves the result. */
    suspend fun feed(nowMillis: Long = System.currentTimeMillis()): CatState {
        val fed = (loadCurrent(nowMillis) ?: CatState()).copy(asleep = false).fed()
        save(fed, nowMillis)
        return fed
    }

    /**
     * Saves locally and shares with the other device. [urgent] sends it right away; routine
     * autosaves pass false and let the system batch them to save battery.
     */
    suspend fun save(state: CatState, nowMillis: Long, urgent: Boolean = true) {
        val saved = SavedCat(state, nowMillis)
        context.catDataStore.edit { it.write(saved) }
        sync.publish(saved, urgent)
    }

    private fun MutablePreferences.write(saved: SavedCat) {
        this[FULLNESS] = saved.state.fullness
        this[ENERGY] = saved.state.energy
        this[MOOD] = saved.state.mood
        this[ASLEEP] = saved.state.asleep
        this[SAVED_AT] = saved.savedAtMillis
    }

    private companion object {
        val FULLNESS = floatPreferencesKey("fullness")
        val ENERGY = floatPreferencesKey("energy")
        val MOOD = floatPreferencesKey("mood")
        val ASLEEP = booleanPreferencesKey("asleep")
        val SAVED_AT = longPreferencesKey("saved_at")
    }
}
