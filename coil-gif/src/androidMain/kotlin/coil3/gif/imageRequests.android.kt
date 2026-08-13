package coil3.gif

import android.os.Build.VERSION.SDK_INT
import coil3.request.ImageRequest
import coil3.request.Options

// region Binary compatibility shims

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

internal actual fun validateRepeatCount(repeatCount: Int) {
    val minimum = if (SDK_INT >= 28) {
        AnimatedImageDecoder.ENCODED_LOOP_COUNT
    } else {
        MovieDrawable.REPEAT_INFINITE
    }
    require(repeatCount >= minimum) { "Invalid repeatCount: $repeatCount" }
}
