package com.kamino.howanyalife.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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

class CatRepository(private val context: Context) {

    suspend fun load(): SavedCat? = context.catDataStore.data.first().toSavedCat()

    /** Emits the saved state every time it changes, e.g. so the widget redraws after feeding. */
    fun observe(): Flow<SavedCat?> = context.catDataStore.data.map { it.toSavedCat() }

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

    suspend fun save(state: CatState, nowMillis: Long) {
        context.catDataStore.edit { prefs ->
            prefs[FULLNESS] = state.fullness
            prefs[ENERGY] = state.energy
            prefs[MOOD] = state.mood
            prefs[ASLEEP] = state.asleep
            prefs[SAVED_AT] = nowMillis
        }
    }

    private companion object {
        val FULLNESS = floatPreferencesKey("fullness")
        val ENERGY = floatPreferencesKey("energy")
        val MOOD = floatPreferencesKey("mood")
        val ASLEEP = booleanPreferencesKey("asleep")
        val SAVED_AT = longPreferencesKey("saved_at")
    }
}
