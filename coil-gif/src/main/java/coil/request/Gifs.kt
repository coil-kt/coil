@file:Suppress("UNCHECKED_CAST", "unused")
@file:JvmName("Gifs")

package coil.request

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoilApi
import coil.decode.GifDecoder.Companion.ANIMATED_TRANSFORMATION_KEY
import coil.decode.GifDecoder.Companion.ANIMATION_END_CALLBACK_KEY
import coil.decode.GifDecoder.Companion.ANIMATION_START_CALLBACK_KEY
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

/**
 * Get the number of times to repeat the animation if the result is an animated [Drawable].
 */
fun Parameters.repeatCount(): Int? = value(REPEAT_COUNT_KEY) as Int?

/**
 * Set the [AnimatedTransformation] that will be applied to the result if it is an animated [Drawable].
 *
 * Default: `null`
 *
 * @see MovieDrawable.setAnimatedTransformation
 * @see ImageDecoder.setPostProcessor
 */
@ExperimentalCoilApi
fun ImageRequest.Builder.animatedTransformation(animatedTransformation: AnimatedTransformation): ImageRequest.Builder {
    return setParameter(ANIMATED_TRANSFORMATION_KEY, animatedTransformation)
}

/**
 * Get the [AnimatedTransformation] that will be applied to the result if it is an animated [Drawable].
 */
@ExperimentalCoilApi
fun Parameters.animatedTransformation(): AnimatedTransformation? {
    return value(ANIMATED_TRANSFORMATION_KEY) as AnimatedTransformation?
}

/**
 * Set the callback to be invoked at the start of the animation if the result is an animated [Drawable].
 */
fun ImageRequest.Builder.onAnimationStart(callback: (() -> Unit)?): ImageRequest.Builder {
    return setParameter(ANIMATION_START_CALLBACK_KEY, callback)
}

/**
 * Get the callback to be invoked at the start of the animation if the result is an animated [Drawable].
 */
fun Parameters.animationStartCallback(): (() -> Unit)? {
    return value(ANIMATION_START_CALLBACK_KEY) as (() -> Unit)?
}

/**
 * Set the callback to be invoked at the end of the animation if the result is an animated [Drawable].
 */
fun ImageRequest.Builder.onAnimationEnd(callback: (() -> Unit)?): ImageRequest.Builder {
    return setParameter(ANIMATION_END_CALLBACK_KEY, callback)
}

/**
 * Get the callback to be invoked at the end of the animation if the result is an animated [Drawable].
 */
fun Parameters.animationEndCallback(): (() -> Unit)? {
    return value(ANIMATION_END_CALLBACK_KEY) as (() -> Unit)?
}
