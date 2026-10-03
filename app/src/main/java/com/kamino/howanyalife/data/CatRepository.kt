package com.kamino.howanyalife.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kamino.howanyalife.model.CatState
import kotlinx.coroutines.flow.first

private val Context.catDataStore by preferencesDataStore(name = "cat")

/** A saved [CatState] plus the wall-clock time it was saved at. */
data class SavedCat(val state: CatState, val savedAtMillis: Long)

class CatRepository(private val context: Context) {

    suspend fun load(): SavedCat? {
        val prefs = context.catDataStore.data.first()
        val savedAt = prefs[SAVED_AT] ?: return null
        val state = CatState(
            fullness = prefs[FULLNESS] ?: return null,
            energy = prefs[ENERGY] ?: return null,
            mood = prefs[MOOD] ?: return null,
            asleep = prefs[ASLEEP] ?: false,
        )
        return SavedCat(state, savedAt)
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
