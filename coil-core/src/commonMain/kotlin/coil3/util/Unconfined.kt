package coil3.util

import coil3.annotation.InternalCoilApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A [CoroutineDispatcher] feature that delegates to [Dispatchers.Unconfined] while
 * [unconfined] is true.
 */
@InternalCoilApi
interface Unconfined {
    var unconfined: Boolean
}
