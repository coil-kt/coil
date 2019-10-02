@file:Suppress("unused")
@file:JvmName("RequestBuilders")

package coil.extension

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.decode.GifDecoder
import coil.drawable.MovieDrawable
import coil.request.RequestBuilder

/**
 * Set the number of times to repeat the animation if the result is an animated [Drawable].
 *
 * Default: [MovieDrawable.REPEAT_INFINITE]
 *
 * @see MovieDrawable.setRepeatCount
 * @see AnimatedImageDrawable.setRepeatCount
 */
fun RequestBuilder<*>.repeatCount(repeatCount: Int) {
    require(repeatCount >= MovieDrawable.REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
    setParameter(GifDecoder.REPEAT_COUNT_KEY, repeatCount, repeatCount.toString())
}
