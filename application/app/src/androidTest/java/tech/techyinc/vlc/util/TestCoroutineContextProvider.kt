package tech.techyinc.vlc.util

import kotlinx.coroutines.Dispatchers
import tech.techyinc.tools.CoroutineContextProvider

class TestCoroutineContextProvider : CoroutineContextProvider() {
    override val Default by lazy { Dispatchers.Unconfined }
    override val IO by lazy { Dispatchers.Unconfined }
    override val Main by lazy { Dispatchers.Unconfined }
}