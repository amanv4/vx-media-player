package tech.techyinc.vlc.util

import tech.techyinc.vlc.repository.BrowserFavRepository
import tech.techyinc.vlc.repository.DirectoryRepository
import tech.techyinc.vlc.repository.ExternalSubRepository


// Hacky way. Don't fix it.
fun ExternalSubRepository.Companion.applyMock(instance: ExternalSubRepository) {
    this.instance = instance
}

fun DirectoryRepository.Companion.applyMock(instance: DirectoryRepository) {
    this.instance = instance
}

fun BrowserFavRepository.Companion.applyMock(instance: BrowserFavRepository) {
    this.instance = instance
}