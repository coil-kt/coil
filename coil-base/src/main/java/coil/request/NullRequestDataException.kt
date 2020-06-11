package coil.request

import coil.ImageLoader
import kotlin.coroutines.CoroutineContext

/**
 * Exception thrown by [ImageLoader.execute] (inside the [CoroutineContext]) when [ImageRequest.data] is null.
 */
class NullRequestDataException : RuntimeException("The request's data is null.")
