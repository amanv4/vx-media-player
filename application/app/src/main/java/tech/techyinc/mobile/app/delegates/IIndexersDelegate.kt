package tech.techyinc.mobile.app.delegates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.launch
import tech.techyinc.moviepedia.MediaScraper
import tech.techyinc.resources.ACTION_CONTENT_INDEXING
import tech.techyinc.tools.AppScope
import tech.techyinc.tools.localBroadcastManager

internal interface IIndexersDelegate {
    fun Context.setupIndexers()
}

internal class IndexersDelegate : BroadcastReceiver(), IIndexersDelegate {

    override fun Context.setupIndexers() {
        localBroadcastManager.registerReceiver(this@IndexersDelegate, IntentFilter(ACTION_CONTENT_INDEXING))
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        AppScope.launch {
            MediaScraper.indexListener.onIndexingDone()
        }
    }
}