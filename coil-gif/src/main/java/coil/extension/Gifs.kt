@file:Suppress("unused")
@file:JvmName("Gifs")

package coil.extension

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.decode.GifDecoder.Companion.REPEAT_COUNT_KEY
import coil.drawable.MovieDrawable
import coil.request.Parameters
import coil.request.RequestBuilder

/**
 * Set the number of times to repeat the animation if the result is an animated [Drawable].
 *
 * Default: [MovieDrawable.REPEAT_INFINITE]
 *
 * @see MovieDrawable.setRepeatCount
 * @see AnimatedImageDrawable.setRepeatCount
 */
fun <T : RequestBuilder<T>> RequestBuilder<T>.repeatCount(repeatCount: Int): T {
    require(repeatCount >= MovieDrawable.REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
    return setParameter(REPEAT_COUNT_KEY, repeatCount)
}

/** Get the number of times to repeat the animation if the result is an animated [Drawable]. */
fun Parameters.repeatCount(): Int? = value(REPEAT_COUNT_KEY) as Int?
