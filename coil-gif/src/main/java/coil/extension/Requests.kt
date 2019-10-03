@file:Suppress("unused")
@file:JvmName("Requests")

package coil.extension

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.drawable.MovieDrawable
import coil.request.Parameters
import coil.request.RequestBuilder

private const val REPEAT_COUNT_KEY = "coil#repeat_count"

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
    setParameter(REPEAT_COUNT_KEY, repeatCount, repeatCount.toString())
}

/** Get the number of times to repeat the animation if the result is an animated [Drawable]. */
fun Parameters.repeatCount(): Int? = value(REPEAT_COUNT_KEY) as? Int
