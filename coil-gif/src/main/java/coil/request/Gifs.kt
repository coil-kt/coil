@file:Suppress("unused")
@file:JvmName("Gifs")

package coil.request

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.decode.GifDecoder.Companion.REPEAT_COUNT_KEY
import coil.drawable.MovieDrawable

/**
 * Set the number of times to repeat the animation if the result is an animated [Drawable].
 *
 * Default: [MovieDrawable.REPEAT_INFINITE]
 *
 * @see MovieDrawable.setRepeatCount
 * @see AnimatedImageDrawable.setRepeatCount
 */
fun ImageRequest.Builder.repeatCount(repeatCount: Int): ImageRequest.Builder {
    require(repeatCount >= MovieDrawable.REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
    return setParameter(REPEAT_COUNT_KEY, repeatCount)
}

/** Get the number of times to repeat the animation if the result is an animated [Drawable]. */
fun Parameters.repeatCount(): Int? = value(REPEAT_COUNT_KEY) as Int?
