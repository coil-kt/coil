package coil.request

import coil.ImageLoader
import kotlin.coroutines.CoroutineContext

/**
 * An exception thrown by [ImageLoader.launch] (inside the [CoroutineContext]) when [Request.data] is null.
 */
class NullRequestDataException : RuntimeException("The request's data is null.")
