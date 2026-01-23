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

// region Binary compatibility shims (Comment #5)

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun ImageRequest.Builder.animatedTransformation(
    animatedTransformation: AnimatedTransformation?,
): ImageRequest.Builder = animatedTransformation(animatedTransformation)

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
val ImageRequest.animatedTransformation: AnimatedTransformation?
    get() = animatedTransformation

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
val Options.animatedTransformation: AnimatedTransformation?
    get() = animatedTransformation

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun ImageRequest.Builder.onAnimationStart(callback: (() -> Unit)?): ImageRequest.Builder =
    onAnimationStart(callback)

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
val ImageRequest.animationStartCallback: (() -> Unit)?
    get() = animationStartCallback

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
val Options.animationStartCallback: (() -> Unit)?
    get() = animationStartCallback

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun ImageRequest.Builder.onAnimationEnd(callback: (() -> Unit)?): ImageRequest.Builder =
    onAnimationEnd(callback)

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
val ImageRequest.animationEndCallback: (() -> Unit)?
    get() = animationEndCallback

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
val Options.animationEndCallback: (() -> Unit)?
    get() = animationEndCallback

// endregion
