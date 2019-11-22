package coil.request

import coil.ImageLoader

/**
 * Exception for when null [Request.data] is passed to [ImageLoader.load].
 *
 * @see Request.fallback
 */
class NullRequestDataException : RuntimeException("The request's data is null.")
