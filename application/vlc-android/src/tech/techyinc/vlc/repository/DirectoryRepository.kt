package tech.techyinc.vlc.repository

import android.content.Context
import kotlinx.coroutines.*
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import tech.techyinc.resources.AndroidDevices
import tech.techyinc.resources.AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY
import tech.techyinc.tools.IOScopedObject
import tech.techyinc.tools.SingletonHolder
import tech.techyinc.vlc.R
import tech.techyinc.vlc.database.CustomDirectoryDao
import tech.techyinc.vlc.database.MediaDatabase
import tech.techyinc.vlc.util.FileUtils
import java.io.File

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DirectoryRepository (private val customDirectoryDao: CustomDirectoryDao) : IOScopedObject() {

    fun addCustomDirectory(path: String): Job = launch {
        customDirectoryDao.insert(tech.techyinc.vlc.mediadb.models.CustomDirectory(path))
    }

    suspend fun getCustomDirectories() = withContext(coroutineContext) {
        try {
            customDirectoryDao.getAll()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteCustomDirectory(path: String) = launch { customDirectoryDao.delete(tech.techyinc.vlc.mediadb.models.CustomDirectory(path)) }

    suspend fun customDirectoryExists(path: String) = withContext(coroutineContext) { customDirectoryDao.get(path).isNotEmpty() }

    suspend fun getMediaDirectoriesList(context: Context) = getMediaDirectories().filter {
        File(it).exists()
    }.map { createDirectory(it, context) }

    suspend fun getMediaDirectories() = mutableListOf<String>().apply {
        add(EXTERNAL_PUBLIC_DIRECTORY)
        addAll(AndroidDevices.externalStorageDirectories)
        addAll(getCustomDirectories().map { it.path })
    }

    companion object : SingletonHolder<DirectoryRepository, Context>({ DirectoryRepository(MediaDatabase.getInstance(it).customDirectoryDao()) })
}

fun createDirectory(it: String, context: Context): MediaWrapper {
    val directory = MLServiceLocator.getAbstractMediaWrapper(AndroidUtil.PathToUri(it))
    directory.type = MediaWrapper.TYPE_DIR
    if (EXTERNAL_PUBLIC_DIRECTORY == it) {
        directory.setDisplayTitle(context.resources.getString(R.string.internal_memory))
    } else {
        val deviceName = FileUtils.getStorageTag(directory.title)
        if (deviceName != null) directory.setDisplayTitle(deviceName)
    }
    return directory
}