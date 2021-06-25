package tech.techyinc.television.viewmodel

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.videolan.medialibrary.media.MediaLibraryItem
import tech.techyinc.vlc.providers.medialibrary.*
import tech.techyinc.resources.CATEGORY_ALBUMS
import tech.techyinc.resources.CATEGORY_ARTISTS
import tech.techyinc.resources.CATEGORY_GENRES
import tech.techyinc.resources.CATEGORY_VIDEOS
import tech.techyinc.vlc.viewmodels.MedialibraryViewModel
import tech.techyinc.vlc.viewmodels.tv.TvBrowserModel


@ExperimentalCoroutinesApi
class MediaBrowserViewModel(context: Context, val category: Long, val parent : MediaLibraryItem?) : MedialibraryViewModel(context), TvBrowserModel<MediaLibraryItem> {


    override var nbColumns = 0
    override var currentItem: MediaLibraryItem? = parent

    override val provider = when (category) {
        CATEGORY_ALBUMS -> AlbumsProvider(parent, context, this)
        CATEGORY_ARTISTS -> ArtistsProvider(context, this, true)
        CATEGORY_GENRES -> GenresProvider(context, this)
        CATEGORY_VIDEOS -> VideosProvider(null, null, context, this)
        else -> TracksProvider(null, context, this)
    }
    override val providers = arrayOf(provider)

    init {
        when(category){
            CATEGORY_ALBUMS -> watchAlbums()
            CATEGORY_ARTISTS -> watchArtists()
            CATEGORY_GENRES -> watchGenres()
            else -> watchMedia()
        }
    }

    class Factory(private val context: Context, private val category: Long, private val parent : MediaLibraryItem?) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MediaBrowserViewModel(context.applicationContext, category, parent) as T
        }
    }
}

@ExperimentalCoroutinesApi
fun Fragment.getMediaBrowserModel(category: Long, parent : MediaLibraryItem? = null) = ViewModelProvider(requireActivity(), MediaBrowserViewModel.Factory(requireContext(), category, parent)).get(MediaBrowserViewModel::class.java)
