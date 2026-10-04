package com.kamino.howanyalife.mobile.sync

import androidx.glance.appwidget.updateAll
import com.kamino.howanyalife.data.CatSyncListenerService
import com.kamino.howanyalife.mobile.widget.HowanyaWidget

/** Receives the cat from the watch and redraws the home-screen widget when it changes. */
class PhoneCatSyncService : CatSyncListenerService() {
    override suspend fun onCatChanged() {
        HowanyaWidget().updateAll(this)
    }
}
