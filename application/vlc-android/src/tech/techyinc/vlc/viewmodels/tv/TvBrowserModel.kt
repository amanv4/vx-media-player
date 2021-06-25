package tech.techyinc.vlc.viewmodels.tv

import tech.techyinc.resources.util.HeaderProvider

interface TvBrowserModel<T> {
    fun isEmpty() : Boolean
    var currentItem: T?
    var nbColumns: Int
    val provider: HeaderProvider
}