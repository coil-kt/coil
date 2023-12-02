@file:Suppress("NOTHING_TO_INLINE")

package coil3

import android.content.Context
import android.widget.ImageView
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.target
import coil3.util.CoilUtils

/**
 * Get the singleton [ImageLoader].
 */
inline val Context.imageLoader: ImageLoader
    get() = SingletonImageLoader.get(this)

/**
 * Load the image referenced by [data] and set it on this [ImageView].
 *
 * Example:
 * ```
 * imageView.load("https://example.com/image.jpg") {
 *     crossfade(true)
 *     transformations(CircleCropTransformation())
 * }
 * ```
 *
 * @param data The data to load.
 * @param imageLoader The [ImageLoader] that will be used to enqueue the [ImageRequest].
 *  By default, the singleton [ImageLoader] will be used.
 * @param builder An optional lambda to configure the [ImageRequest].
 */
inline fun ImageView.load(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}

/**
 * Dispose the request that's attached to this view (if there is one).
 */
inline fun ImageView.dispose() {
    CoilUtils.dispose(this)
}

/**
 * Get the [ImageResult] of the most recently executed image request that's attached to this view.
 */
inline val ImageView.result: ImageResult?
    get() = CoilUtils.result(this)
