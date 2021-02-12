@file:Suppress("unused")
@file:JvmName("Gifs")

package coil.request

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.decode.GifDecoder.Companion.ANIMATED_TRANSFORMATION_KEY
import coil.decode.GifDecoder.Companion.REPEAT_COUNT_KEY
import coil.drawable.MovieDrawable
import coil.transform.AnimatedTransformation

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

/** Set the [AnimatedTransformation] that will be applied to the result if it is an animated [Drawable]. */
fun ImageRequest.Builder.animatedTransformation(animatedTransformation: AnimatedTransformation): ImageRequest.Builder {
    return setParameter(ANIMATED_TRANSFORMATION_KEY, animatedTransformation)
}

/** Get the [AnimatedTransformation] applied to the result if it is an animated [Drawable]. */
fun Parameters.animatedTransformation(): AnimatedTransformation? {
    return value(ANIMATED_TRANSFORMATION_KEY) as AnimatedTransformation?
}
