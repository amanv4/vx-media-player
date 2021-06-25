package tech.techyinc.vlc.viewmodels.browser

import android.os.Handler
import androidx.core.net.toUri
import androidx.lifecycle.MediatorLiveData
import com.jraska.livedata.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.util.MediaBrowser
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.medialibrary.media.MediaWrapperImpl
import tech.techyinc.tools.CoroutineContextProvider
import tech.techyinc.tools.Settings
import tech.techyinc.vlc.BaseTest
import tech.techyinc.vlc.database.BrowserFavDao
import tech.techyinc.vlc.providers.BrowserProvider
import tech.techyinc.vlc.repository.BrowserFavRepository
import tech.techyinc.vlc.util.TestCoroutineContextProvider
import tech.techyinc.vlc.util.applyMock

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class NetworkModelTest : BaseTest() {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val mockedLibVlc: LibVLC = mockk(relaxed = true)
    private val mockedFavoritesDao: BrowserFavDao = mockk(relaxed = true)
    private val mockedFavoritesRepo: BrowserFavRepository = spyk(BrowserFavRepository(mockedFavoritesDao))

    private lateinit var mediaBrowser: MediaBrowser
    private lateinit var browserModel: BrowserModel
    private lateinit var browserProvider: BrowserProvider

    init {
        BrowserFavRepository.applyMock(mockedFavoritesRepo)

        // BrowserHandler mocked.
        val handler: Handler = mockk()

        BrowserProvider.overrideCreator = false
        BrowserProvider.registerCreator {
            mediaBrowser = spyk(MediaBrowser(mockedLibVlc, null, handler))
            mediaBrowser
        }
        BrowserProvider.registerCreator(clazz = CoroutineContextProvider::class.java) { TestCoroutineContextProvider() }
    }

    private fun initNetworkModel(url: String?, showHiddenFiles: Boolean = false) {
        browserModel = NetworkModel(application, url, showHiddenFiles, TestCoroutineContextProvider())
        browserProvider = browserModel.provider
    }

    private fun getFakeMediaWrapper(index: Int): MediaWrapperImpl = MediaWrapperImpl("http://fake_media.io/vid_$index.mp4".toUri())

    @Test
    fun whenAtRootAndNoFavorites_checkResultIsEmpty() {
        Settings.overrideTvUI = true
        every { mockedFavoritesRepo.networkFavorites } returns MediatorLiveData<List<MediaWrapper>>().apply { value = emptyList() }
        initNetworkModel(null)

        val testResult = browserModel.dataset.test()
                .awaitValue()
                .value()

        assertEquals(0, testResult.size)
    }

    @Test
    fun whenAtRootWithOneFavoriteAndOneFavoriteAddedLater_checkResultIsNotEmptyAndContainsThem() {
        val liveFavorites: MediatorLiveData<List<MediaWrapper>> = MediatorLiveData()
        Settings.overrideTvUI = true
        every { mockedFavoritesRepo.networkFavorites } returns liveFavorites

        liveFavorites.value = listOf(getFakeMediaWrapper(0).apply { setStateFlags(MediaLibraryItem.FLAG_FAVORITE) })
        initNetworkModel(null)

        val oldResult = browserModel.dataset.test()
                .awaitValue()
                .value()

        assertEquals(3, oldResult.size)
        assertEquals(getFakeMediaWrapper(0), oldResult[1])

        liveFavorites.value = ArrayList<MediaWrapper>().apply {
            liveFavorites.value?.let { addAll(it) }
            add(getFakeMediaWrapper(1).apply { setStateFlags(MediaLibraryItem.FLAG_FAVORITE) })
        }

        val newResult = browserModel.dataset.test()
                .awaitValue()
                .value()

        assertEquals(4, newResult.size)
        assertEquals(getFakeMediaWrapper(0), newResult[1])
        assertEquals(getFakeMediaWrapper(1), newResult[2])
    }
}