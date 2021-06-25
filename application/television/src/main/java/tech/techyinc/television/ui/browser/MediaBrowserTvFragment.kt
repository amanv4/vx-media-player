package tech.techyinc.television.ui.browser

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import tech.techyinc.resources.*
import tech.techyinc.television.ui.MediaTvItemAdapter
import tech.techyinc.television.ui.TvItemAdapter
import tech.techyinc.television.ui.TvUtil
import tech.techyinc.television.viewmodel.MediaBrowserViewModel
import tech.techyinc.television.viewmodel.getMediaBrowserModel
import tech.techyinc.tools.FORCE_PLAY_ALL
import tech.techyinc.tools.Settings
import tech.techyinc.vlc.R
import tech.techyinc.vlc.gui.view.EmptyLoadingState
import tech.techyinc.vlc.interfaces.IEventsHandler
import tech.techyinc.vlc.providers.medialibrary.MedialibraryProvider

@UseExperimental(ObsoleteCoroutinesApi::class)
@ExperimentalCoroutinesApi
class MediaBrowserTvFragment : BaseBrowserTvFragment<MediaLibraryItem>() {
    override fun provideAdapter(eventsHandler: IEventsHandler<MediaLibraryItem>, itemSize: Int): TvItemAdapter {
        return MediaTvItemAdapter(when ((viewModel as MediaBrowserViewModel).category) {
            CATEGORY_SONGS -> MediaWrapper.TYPE_AUDIO
            CATEGORY_ALBUMS -> MediaWrapper.TYPE_ALBUM
            CATEGORY_ARTISTS -> MediaWrapper.TYPE_ARTIST
            CATEGORY_GENRES -> MediaWrapper.TYPE_GENRE
            else -> MediaWrapper.TYPE_VIDEO
        }, this, itemSize)
    }

    override fun getDisplayPrefId() = "display_tv_media_${(viewModel as MediaBrowserViewModel).category}"

    override lateinit var adapter: TvItemAdapter

    override fun getTitle() = when ((viewModel as MediaBrowserViewModel).category) {
        CATEGORY_SONGS -> getString(R.string.tracks)
        CATEGORY_ALBUMS -> getString(R.string.albums)
        CATEGORY_ARTISTS -> getString(R.string.artists)
        CATEGORY_GENRES -> getString(R.string.genres)
        else -> getString(R.string.video)
    }

    override fun getCategory(): Long = (viewModel as MediaBrowserViewModel).category

    override fun getColumnNumber() = when ((viewModel as MediaBrowserViewModel).category) {
        CATEGORY_VIDEOS -> resources.getInteger(R.integer.tv_videos_col_count)
        else -> resources.getInteger(R.integer.tv_songs_col_count)
    }

    companion object {
        fun newInstance(type: Long, item: MediaLibraryItem?) =
                MediaBrowserTvFragment().apply {
                    arguments = bundleOf(CATEGORY to type, ITEM to item)
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentItem = if (savedInstanceState != null) savedInstanceState.getParcelable<Parcelable>(ITEM) as? MediaLibraryItem
        else requireActivity().intent.getParcelableExtra<Parcelable>(ITEM) as? MediaLibraryItem

        viewModel = getMediaBrowserModel(arguments?.getLong(CATEGORY, CATEGORY_SONGS) ?: CATEGORY_SONGS, currentItem)

        (viewModel.provider as MedialibraryProvider<*>).pagedList.observe(this, { items ->
            submitList(items)

            binding.emptyLoading.state = if (items.isEmpty()) EmptyLoadingState.EMPTY else EmptyLoadingState.NONE

            //headers
            val nbColumns = if ((viewModel as MediaBrowserViewModel).sort == Medialibrary.SORT_ALPHA || (viewModel as MediaBrowserViewModel).sort == Medialibrary.SORT_DEFAULT) 9 else 1

            binding.headerList.layoutManager = GridLayoutManager(requireActivity(), nbColumns)
            headerAdapter.sortType = (viewModel as MediaBrowserViewModel).sort
        })

        viewModel.provider.liveHeaders.observe(this, {
            updateHeaders(it)
            binding.list.invalidateItemDecorations()
        })

        (viewModel.provider as MedialibraryProvider<*>).loading.observe(this, {
            binding.emptyLoading.state = when {
                it -> EmptyLoadingState.LOADING
                viewModel.isEmpty() && adapter.isEmpty() -> EmptyLoadingState.EMPTY
                else -> EmptyLoadingState.NONE
            }
        })
    }

    override fun onClick(v: View, position: Int, item: MediaLibraryItem) {
        lifecycleScope.launchWhenStarted {
            if ((viewModel as MediaBrowserViewModel).category == CATEGORY_VIDEOS && !Settings.getInstance(requireContext()).getBoolean(FORCE_PLAY_ALL, true)) {
                TvUtil.playMedia(requireActivity(), item as MediaWrapper)
            } else {
                TvUtil.openMediaFromPaged(requireActivity(), item, viewModel.provider as MedialibraryProvider<out MediaLibraryItem>)
            }
        }
    }
}
