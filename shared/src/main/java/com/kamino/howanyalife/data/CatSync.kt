package com.kamino.howanyalife.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.kamino.howanyalife.model.CatState
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.max

/**
 * Combines two snapshots of the same cat saved on different devices.
 *
 * The newer snapshot wins, except that fullness and mood take the higher of the two once the
 * older one has been brought up to the same moment: feeding or petting on one device is never
 * lost to a save that happened at nearly the same time on the other. The result does not depend
 * on argument order and merging it again changes nothing, so both devices settle on the same cat.
 */
fun mergeCats(
    a: SavedCat,
    b: SavedCat,
    advance: (CatState, Long) -> CatState = { state, millis -> state.advance(catTime(millis)) },
): SavedCat {
    val (older, newer) = if (a.savedAtMillis <= b.savedAtMillis) a to b else b to a
    val caughtUp = advance(older.state, newer.savedAtMillis - older.savedAtMillis)
    val state = newer.state.copy(
        fullness = max(newer.state.fullness, caughtUp.fullness),
        mood = max(newer.state.mood, caughtUp.mood),
    )
    return SavedCat(state, newer.savedAtMillis)
}

/** True when two snapshots describe the same cat (ignoring float rounding). */
fun SavedCat.sameAs(other: SavedCat?): Boolean =
    other != null &&
        savedAtMillis == other.savedAtMillis &&
        state.asleep == other.state.asleep &&
        abs(state.fullness - other.state.fullness) < 0.01f &&
        abs(state.energy - other.state.energy) < 0.01f &&
        abs(state.mood - other.state.mood) < 0.01f

/**
 * Shares the cat between the phone and the watch through the Wearable Data Layer.
 *
 * Each device publishes its latest snapshot as a data item at [PATH]; the other device's
 * [CatSyncListenerService] (or a running app, via [fetchAll]) merges it into its own state.
 * Both apps must have the same application ID and signing key for the Data Layer to connect them.
 */
class CatSync(context: Context) {

    private val dataClient = Wearable.getDataClient(context)

    suspend fun publish(saved: SavedCat, urgent: Boolean) {
        try {
            val request = PutDataMapRequest.create(PATH).apply { saved.writeTo(dataMap) }.asPutDataRequest()
            if (urgent) request.setUrgent()
            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            // No paired device or no Google Play services: the cat simply stays local.
            Log.w(TAG, "Could not publish the cat", e)
        }
    }

    /** Snapshots currently in the Data Layer, from every connected device (including this one). */
    suspend fun fetchAll(): List<SavedCat> = try {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(PATH).build()
        val items = dataClient.getDataItems(uri).await()
        try {
            items.mapNotNull { item -> item.data?.let { DataMap.fromByteArray(it).toSavedCat() } }
        } finally {
            items.release()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not read the cat from the Data Layer", e)
        emptyList()
    }

    companion object {
        const val PATH = "/cat/state"
        private const val TAG = "CatSync"

        private const val FULLNESS = "fullness"
        private const val ENERGY = "energy"
        private const val MOOD = "mood"
        private const val ASLEEP = "asleep"
        private const val SAVED_AT = "savedAt"

        private fun SavedCat.writeTo(map: DataMap) {
            map.putFloat(FULLNESS, state.fullness)
            map.putFloat(ENERGY, state.energy)
            map.putFloat(MOOD, state.mood)
            map.putBoolean(ASLEEP, state.asleep)
            map.putLong(SAVED_AT, savedAtMillis)
        }

        fun DataMap.toSavedCat(): SavedCat? {
            if (!containsKey(SAVED_AT)) return null
            return SavedCat(
                CatState(
                    fullness = getFloat(FULLNESS),
                    energy = getFloat(ENERGY),
                    mood = getFloat(MOOD),
                    asleep = getBoolean(ASLEEP),
                ),
                getLong(SAVED_AT),
            )
        }
    }
}
