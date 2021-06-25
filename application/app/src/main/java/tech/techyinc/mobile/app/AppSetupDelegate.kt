/*****************************************************************************
 * AppSetupDelegate.ki
 *
 * Copyright © 2020 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package tech.techyinc.mobile.app

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.videolan.libvlc.Dialog
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.LibVLCFactory
import org.videolan.libvlc.MediaFactory
import org.videolan.libvlc.interfaces.ILibVLCFactory
import org.videolan.libvlc.interfaces.IMediaFactory
import tech.techyinc.mobile.app.delegates.IIndexersDelegate
import tech.techyinc.mobile.app.delegates.IMediaContentDelegate
import tech.techyinc.mobile.app.delegates.IndexersDelegate
import tech.techyinc.mobile.app.delegates.MediaContentDelegate
import tech.techyinc.resources.AppContextProvider
import tech.techyinc.resources.VLCInstance
import tech.techyinc.tools.AppScope
import tech.techyinc.tools.Settings
import tech.techyinc.vlc.BuildConfig
import tech.techyinc.vlc.gui.SendCrashActivity
import tech.techyinc.vlc.gui.helpers.NotificationHelper
import tech.techyinc.vlc.util.DialogDelegate
import tech.techyinc.vlc.util.VersionMigration

interface AppDelegate {
    val appContextProvider : AppContextProvider
    fun Context.setupApplication()
}

class AppSetupDelegate : AppDelegate,
        IMediaContentDelegate by MediaContentDelegate(),
        IIndexersDelegate by IndexersDelegate() {

    // Store AppContextProvider to prevent GC
    override val appContextProvider = AppContextProvider

    @TargetApi(Build.VERSION_CODES.O)
    override fun Context.setupApplication() {
        appContextProvider.init(this)
        NotificationHelper.createNotificationChannels(this)

        // Service loaders
        FactoryManager.registerFactory(IMediaFactory.factoryId, MediaFactory())
        FactoryManager.registerFactory(ILibVLCFactory.factoryId, LibVLCFactory())
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)

        if (BuildConfig.DEBUG) {
            Settings.getInstance(this)
            if (Settings.showTvUi) {
                // Register movipedia to resume tv shows/movies
                setupContentResolvers()

                // Setup Moviepedia indexing after Medialibrary scan
                setupIndexers()
            }
        }
        AppContextProvider.setLocale(Settings.getInstance(this).getString("set_locale", ""))

        //Initiate Kotlinx Dispatchers in a thread to prevent ANR
        backgroundInit()
    }

    // init operations executed in background threads
    private fun Context.backgroundInit() = AppScope.launch {
        launch(Dispatchers.IO) {
            if (!VLCInstance.testCompatibleCPU(AppContextProvider.appContext)) return@launch
            Dialog.setCallbacks(VLCInstance.getInstance(this@backgroundInit), DialogDelegate)
        }
        packageManager.setComponentEnabledSetting(ComponentName(this@backgroundInit, SendCrashActivity::class.java),
                if (BuildConfig.BETA) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        VersionMigration.migrateVersion(this@backgroundInit)

    }
}
