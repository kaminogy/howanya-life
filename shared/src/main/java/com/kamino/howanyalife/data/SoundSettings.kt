package com.kamino.howanyalife.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** What the app plays. The sound button cycles through these in order. */
enum class SoundMode {
    /** Background music and sound effects. */
    ALL,
    /** Sound effects only. */
    EFFECTS_ONLY,
    OFF,
    ;

    fun next(): SoundMode = entries[(ordinal + 1) % entries.size]
}

/** The saved [SoundMode]; [default] applies until the user picks one (the watch starts without BGM). */
class SoundSettings(private val context: Context, val default: SoundMode) {

    val mode: Flow<SoundMode> = context.settingsDataStore.data.map { prefs ->
        prefs[SOUND_MODE]?.let { saved -> SoundMode.entries.firstOrNull { it.name == saved } } ?: default
    }

    suspend fun set(mode: SoundMode) {
        context.settingsDataStore.edit { it[SOUND_MODE] = mode.name }
    }

    private companion object {
        val SOUND_MODE = stringPreferencesKey("sound_mode")
    }
}
