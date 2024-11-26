package coil3.gif

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil3.Extras
import coil3.getExtra
import coil3.gif.MovieDrawable.Companion.REPEAT_INFINITE
import coil3.request.ImageRequest
import coil3.request.Options

/**
 * Set the number of times to repeat the animation if the result is an animated [Drawable].
 *
 * @see MovieDrawable.setRepeatCount
 * @see AnimatedImageDrawable.setRepeatCount
 */
fun ImageRequest.Builder.repeatCount(repeatCount: Int) = apply {
    require(repeatCount >= REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
    extras[repeatCountKey] = repeatCount
}

val ImageRequest.repeatCount: Int
    get() = getExtra(repeatCountKey)

val Options.repeatCount: Int
    get() = getExtra(repeatCountKey)

val Extras.Key.Companion.repeatCount: Extras.Key<Int>
    get() = repeatCountKey

private val repeatCountKey = Extras.Key(default = REPEAT_INFINITE)
