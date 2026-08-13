package coil3.gif

import coil3.Extras
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options

/**
 * Set the number of times to repeat the animation if the result is an animated image.
 *
 * A value of 0 plays the animation once, a positive value repeats it that many times after its
 * first iteration, and -1 repeats it indefinitely. Platform decoders can also expose a constant
 * that preserves the loop count encoded in the image.
 */
fun ImageRequest.Builder.repeatCount(repeatCount: Int) = apply {
    validateRepeatCount(repeatCount)
    extras[repeatCountKey] = repeatCount
}

fun ImageLoader.Builder.repeatCount(repeatCount: Int) = apply {
    validateRepeatCount(repeatCount)
    extras[repeatCountKey] = repeatCount
}

val ImageRequest.repeatCount: Int
    get() = getExtra(repeatCountKey)

val Options.repeatCount: Int
    get() = getExtra(repeatCountKey)

val Extras.Key.Companion.repeatCount: Extras.Key<Int>
    get() = repeatCountKey

private val repeatCountKey = Extras.Key(default = REPEAT_INFINITE)

/**
 * Set the [AnimatedTransformation] that will be applied to the result if it is an animated image.
 */
@ExperimentalCoilApi
fun ImageRequest.Builder.animatedTransformation(
    animatedTransformation: AnimatedTransformation?,
) = apply {
    extras[animatedTransformationKey] = animatedTransformation
}

@ExperimentalCoilApi
val ImageRequest.animatedTransformation: AnimatedTransformation?
    get() = getExtra(animatedTransformationKey)

@ExperimentalCoilApi
val Options.animatedTransformation: AnimatedTransformation?
    get() = getExtra(animatedTransformationKey)

@ExperimentalCoilApi
val Extras.Key.Companion.animatedTransformation: Extras.Key<AnimatedTransformation?>
    get() = animatedTransformationKey

private val animatedTransformationKey = Extras.Key<AnimatedTransformation?>(default = null)

/**
 * Set the callback to be invoked at the start of the animation if the result is an animated image.
 */
fun ImageRequest.Builder.onAnimationStart(callback: (() -> Unit)?) = apply {
    extras[animationStartCallbackKey] = callback
}

val ImageRequest.animationStartCallback: (() -> Unit)?
    get() = getExtra(animationStartCallbackKey)

val Options.animationStartCallback: (() -> Unit)?
    get() = getExtra(animationStartCallbackKey)

val Extras.Key.Companion.animationStartCallback: Extras.Key<(() -> Unit)?>
    get() = animationStartCallbackKey

private val animationStartCallbackKey = Extras.Key<(() -> Unit)?>(default = null)

/**
 * Set the callback to be invoked at the end of the animation if the result is an animated image.
 */
fun ImageRequest.Builder.onAnimationEnd(callback: (() -> Unit)?) = apply {
    extras[animationEndCallbackKey] = callback
}

val ImageRequest.animationEndCallback: (() -> Unit)?
    get() = getExtra(animationEndCallbackKey)

val Options.animationEndCallback: (() -> Unit)?
    get() = getExtra(animationEndCallbackKey)

val Extras.Key.Companion.animationEndCallback: Extras.Key<(() -> Unit)?>
    get() = animationEndCallbackKey

private val animationEndCallbackKey = Extras.Key<(() -> Unit)?>(default = null)

internal expect fun validateRepeatCount(repeatCount: Int)

internal const val ENCODED_LOOP_COUNT = -2
internal const val REPEAT_INFINITE = -1
