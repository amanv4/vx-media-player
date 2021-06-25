/*
 * ************************************************************************
 *  BookmarkAdapter.kt
 * *************************************************************************
 * Copyright © 2021 VLC authors and VideoLAN
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

package tech.techyinc.vlc.gui.helpers

import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.medialibrary.interfaces.media.Bookmark
import tech.techyinc.resources.UPDATE_PAYLOAD
import tech.techyinc.tools.Settings
import tech.techyinc.vlc.R
import tech.techyinc.vlc.databinding.BookmarkItemBinding
import tech.techyinc.vlc.gui.DiffUtilAdapter

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class BookmarkAdapter(val bookmarkManager: IBookmarkManager) :
    DiffUtilAdapter<Bookmark, BookmarkAdapter.ViewHolder>() {
    private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.bookmark_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = getItem(position)
        holder.binding.bookmark = bookmark

        holder.binding.executePendingBindings()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) onBindViewHolder(holder, position)
        else for (payload in payloads) {
            holder.binding.bookmark = dataset[position]

        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (Settings.listTitleEllipsize == 4) enableMarqueeEffect(recyclerView, handler)
    }

    override fun getItemCount() = dataset.size

    @MainThread
    override fun getItem(position: Int) = dataset[position]

    inner class ViewHolder @TargetApi(Build.VERSION_CODES.M)
    constructor(v: View) : RecyclerView.ViewHolder(v), MarqueeViewHolder {
        var binding: BookmarkItemBinding = DataBindingUtil.bind(v)!!
        override val titleView = binding.audioItemTitle

        init {
            binding.holder = this
        }

        fun onClick(@Suppress("UNUSED_PARAMETER") v: View, bookmark: Bookmark) {
            bookmarkManager.onBookmarkClick(layoutPosition, bookmark)
        }

        fun onMoreClick(v: View, bookmark: Bookmark) {
            bookmarkManager.onPopupMenu(v, layoutPosition, bookmark)
        }
    }

    interface IBookmarkManager {
        fun onPopupMenu(view: View, position: Int, item: Bookmark?)
        fun onBookmarkClick(position: Int, item: Bookmark)
    }

    override fun createCB(): DiffCallback<Bookmark> = object : DiffCallback<Bookmark>() {
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].title == newList[newItemPosition].title &&
                    oldList[oldItemPosition].time == newList[newItemPosition].time

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return try {
                 oldList[oldItemPosition] == newList[newItemPosition]
            } catch (e: Exception) {
                false
            }
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int) = arrayListOf(
            UPDATE_PAYLOAD
        )
    }
}