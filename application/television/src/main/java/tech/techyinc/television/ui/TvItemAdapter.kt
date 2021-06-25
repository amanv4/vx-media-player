package tech.techyinc.television.ui

interface TvItemAdapter : TvFocusableAdapter {
    fun submitList(pagedList: Any?)
    fun isEmpty() : Boolean
    var focusNext: Int
    fun displaySwitch(inGrid: Boolean)
}
