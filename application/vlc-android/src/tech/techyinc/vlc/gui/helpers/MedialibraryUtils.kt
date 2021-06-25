package tech.techyinc.vlc.gui.helpers


import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.videolan.medialibrary.interfaces.Medialibrary
import tech.techyinc.resources.ACTION_DISCOVER
import tech.techyinc.resources.ACTION_DISCOVER_DEVICE
import tech.techyinc.resources.AppContextProvider
import tech.techyinc.resources.EXTRA_PATH
import tech.techyinc.resources.util.launchForeground
import tech.techyinc.tools.runIO
import tech.techyinc.tools.stripTrailingSlash
import tech.techyinc.vlc.MediaParsingService

object MedialibraryUtils {

    fun removeDir(path: String) {
        runIO(Runnable { Medialibrary.getInstance().removeFolder(path) })
    }

    @JvmOverloads
    fun addDir(path: String, context: Context = AppContextProvider.appContext) {
        val intent = Intent(ACTION_DISCOVER, null, context, MediaParsingService::class.java)
        intent.putExtra(EXTRA_PATH, path)
        context.launchForeground(context, intent)
    }

    fun addDevice(path: String, context: Context) {
        val intent = Intent(ACTION_DISCOVER_DEVICE, null, context, MediaParsingService::class.java)
        intent.putExtra(EXTRA_PATH, path)
        context.launchForeground(context, intent)
    }

    fun isScanned(path: String): Boolean {
        //scheme is supported => test if the parent is scanned
        var isScanned = false
        Medialibrary.getInstance().foldersList.forEach search@{
            if (path.stripTrailingSlash().startsWith(it.toUri().toString().stripTrailingSlash())) {
                isScanned = true
                return@search
            }
        }
        return isScanned
    }
}
