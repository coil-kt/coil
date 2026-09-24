package coil3.compose

import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.ui.layout.ContentScale
import coil3.request.SuccessResult
import coil3.request.transitionFactory
import coil3.transition.CrossfadeTransition
import coil3.transition.TransitionTarget
import kotlin.time.Duration.Companion.milliseconds

internal actual fun maybeNewCrossfadePainter(
    previous: AsyncImagePainter.State,
    current: AsyncImagePainter.State,
    contentScale: ContentScale,
): CrossfadePainter? {
    // We can only invoke the transition factory if the state is success or error.
    val result = when (current) {
        is AsyncImagePainter.State.Success -> current.result
        is AsyncImagePainter.State.Error -> current.result
        else -> return null
    }

    val factory = result.request.transitionFactory as? CrossfadeTransition.Factory ?: return null

    // For errors and cache hits, create() returns a non-crossfade transition. Still crossfade
    // cache hits when the caller opted in via useExistingImageAsPlaceholder.
    val transition = factory.create(FakeTransitionTarget, result)
    if (transition !is CrossfadeTransition && !crossfadeFromExistingImage(previous, result)) {
        return null
    }

    return CrossfadePainter(
        start = previous.painter.takeIf { previous is AsyncImagePainter.State.Loading },
        end = current.painter,
        contentScale = contentScale,
        duration = factory.durationMillis.milliseconds,
        fadeStart = result !is SuccessResult || !result.isPlaceholderCached,
        preferExactIntrinsicSize = factory.preferExactIntrinsicSize,
        preferEndFirstIntrinsicSize = result.request.preferEndFirstIntrinsicSize,
    )
}

private val FakeTransitionTarget = object : TransitionTarget {
    override val view: View get() = throw UnsupportedOperationException()
    override val drawable: Drawable? get() = null
}
