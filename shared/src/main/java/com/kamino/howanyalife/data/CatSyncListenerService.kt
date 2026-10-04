package com.kamino.howanyalife.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.kamino.howanyalife.data.CatSync.Companion.toSavedCat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/**
 * Receives the cat from the other device while this app is closed and merges it into the
 * local state. Each app declares it (or a subclass) in its manifest.
 */
open class CatSyncListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val remote = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == CatSync.PATH }
            .mapNotNull { event ->
                event.dataItem.uri.host to DataMapItem.fromDataItem(event.dataItem).dataMap.toSavedCat()
            }
        if (remote.isEmpty()) return

        // Called on a background thread, so blocking here is fine.
        runBlocking {
            val localNode = Wearable.getNodeClient(this@CatSyncListenerService).localNode.await().id
            val repository = CatRepository(this@CatSyncListenerService)
            var changed = false
            for ((node, cat) in remote) {
                if (node != localNode && cat != null) changed = repository.mergeRemote(cat) || changed
            }
            if (changed) onCatChanged()
        }
    }

    /** Called after a cat from the other device changed the local state. */
    protected open suspend fun onCatChanged() = Unit
}
