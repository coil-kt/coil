package coil.request

import coil.ImageLoader
import kotlin.coroutines.CoroutineContext

/**
 * An exception thrown by [ImageLoader.launch] (inside the [CoroutineContext]) when [LoadRequest.data] is null.
 *
 * @see Request.fallback
 */
class NullRequestDataException : RuntimeException("The request's data is null.")
