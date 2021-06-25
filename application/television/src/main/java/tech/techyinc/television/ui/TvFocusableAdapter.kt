package tech.techyinc.television.ui

import tech.techyinc.resources.interfaces.FocusListener

/**
 * Callback used when the adapter is used in a [FocusableRecyclerView]
 */
interface TvFocusableAdapter {
    fun setOnFocusChangeListener(focusListener: FocusListener?)
}