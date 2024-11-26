package coil3.gif

import coil3.Extras
import coil3.annotation.ExperimentalCoilApi
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options

/**
 * Set the [AnimatedTransformation] that will be applied to the result if it is an animated drawable.
 */
@ExperimentalCoilApi
fun ImageRequest.Builder.animatedTransformation(
    animatedTransformation: AnimatedTransformation?,
) = apply {
    extras[animatedTransformationKey] = animatedTransformation
}

val ImageRequest.animatedTransformation: AnimatedTransformation?
    get() = getExtra(animatedTransformationKey)

val Options.animatedTransformation: AnimatedTransformation?
    get() = getExtra(animatedTransformationKey)

val Extras.Key.Companion.animatedTransformation: Extras.Key<AnimatedTransformation?>
    get() = animatedTransformationKey

private val animatedTransformationKey = Extras.Key<AnimatedTransformation?>(default = null)

/**
 * Set the callback to be invoked at the start of the animation if the result is an animated drawable.
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
 * Set the callback to be invoked at the end of the animation if the result is an animated drawable.
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
