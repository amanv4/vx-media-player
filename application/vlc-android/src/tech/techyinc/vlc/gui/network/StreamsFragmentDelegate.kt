/*
 * ************************************************************************
 *  StreamManagerFragment.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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
 * **************************************************************************
 *
 *
 */

package tech.techyinc.vlc.gui.network

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import tech.techyinc.resources.*
import tech.techyinc.tools.copy
import tech.techyinc.vlc.R
import tech.techyinc.vlc.gui.dialogs.CtxActionReceiver
import tech.techyinc.vlc.gui.dialogs.RenameDialog
import tech.techyinc.vlc.gui.dialogs.SavePlaylistDialog
import tech.techyinc.vlc.gui.helpers.UiTools
import tech.techyinc.vlc.gui.helpers.UiTools.addToPlaylist
import tech.techyinc.vlc.gui.video.VideoPlayerActivity
import tech.techyinc.vlc.media.MediaUtils
import tech.techyinc.vlc.viewmodels.StreamsModel

class StreamsFragmentDelegate : IStreamsFragmentDelegate, CtxActionReceiver {
    private lateinit var keyboardListener: KeyboardListener
    private lateinit var viewModel: StreamsModel
    private lateinit var fragment: Fragment

    override fun setup(fragment: Fragment, viewModel: StreamsModel, keyboardListener: KeyboardListener) {
        this.fragment = fragment
        this.viewModel = viewModel
        this.keyboardListener = keyboardListener
    }

    override fun onCtxAction(position: Int, option: Long) {
        when (option) {
            CTX_RENAME -> renameStream(position)
            CTX_APPEND -> {
                val media = viewModel.dataset.get(position)
                MediaUtils.appendMedia(fragment.requireContext(), media)
            }
            CTX_ADD_TO_PLAYLIST -> {
                val media = viewModel.dataset.get(position)
                fragment.requireActivity().addToPlaylist(media.tracks, SavePlaylistDialog.KEY_NEW_TRACKS)
            }
            CTX_COPY -> {
                val media = viewModel.dataset.get(position)
                fragment.requireContext().copy(media.title, media.location)
                Snackbar.make(fragment.requireActivity().window.decorView.findViewById<View>(android.R.id.content), R.string.url_copied_to_clipboard, Snackbar.LENGTH_LONG).show()
            }
            CTX_DELETE -> {
                val media = viewModel.dataset.get(position)
                viewModel.deletingMedia = media
                UiTools.snackerWithCancel(fragment.requireActivity(), fragment.requireActivity().getString(R.string.stream_deleted), { viewModel.delete() }, {
                    viewModel.deletingMedia = null
                    viewModel.refresh()
                })
                viewModel.refresh()
            }
        }
    }

    override fun showContext(position: Int) {
        val flags = CTX_RENAME or CTX_APPEND or CTX_ADD_TO_PLAYLIST or CTX_COPY or CTX_DELETE
        val media = viewModel.dataset.get(position)
        tech.techyinc.vlc.gui.dialogs.showContext(fragment.requireActivity(), this, position, media.title, flags)
    }

    override fun getlistEventActor(): SendChannel<MrlAction> = fragment.lifecycleScope.actor {
        for (event in channel) when (event) {
            is Playmedia -> playMedia(event.media)
            is ShowContext -> showContext(event.position)
        }
    }

    override fun playMedia(mw: MediaWrapper) {
        mw.type = MediaWrapper.TYPE_STREAM
        if (mw.uri.scheme?.startsWith("rtsp") == true) VideoPlayerActivity.start(fragment.requireContext(), mw.uri)
        else MediaUtils.openMedia(fragment.activity, mw)
        fragment.activity?.invalidateOptionsMenu()
        keyboardListener.hideKeyboard()
    }

    private fun renameStream(position: Int) {
        val dialog = RenameDialog.newInstance(viewModel.dataset.get(position))
        dialog.setListener { media, name -> viewModel.rename(media as MediaWrapper, name) }
        dialog.show(fragment.requireActivity().supportFragmentManager, RenameDialog::class.simpleName)
    }
}

interface KeyboardListener {
    fun hideKeyboard()
}

interface IStreamsFragmentDelegate : CtxActionReceiver {
    fun setup(fragment: Fragment, viewModel: StreamsModel, keyboardListener: KeyboardListener)
    fun showContext(position: Int)
    fun getlistEventActor(): SendChannel<MrlAction>
    fun playMedia(mw: MediaWrapper)
}