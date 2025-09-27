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

    // Invoke the transition factory and wrap the painter in a `CrossfadePainter` if it returns
    // a `CrossfadeTransformation`.
    val transition = result.request.transitionFactory.create(FakeTransitionTarget, result)
    if (transition is CrossfadeTransition) {
        return CrossfadePainter(
            start = previous.painter.takeIf { previous is AsyncImagePainter.State.Loading },
            end = current.painter,
            contentScale = contentScale,
            duration = transition.durationMillis.milliseconds,
            fadeStart = result !is SuccessResult || !result.isPlaceholderCached,
            preferExactIntrinsicSize = transition.preferExactIntrinsicSize,
            preferEndFirstIntrinsicSize = result.request.preferEndFirstIntrinsicSize
        )
    } else {
        return null
    }
}

private val FakeTransitionTarget = object : TransitionTarget {
    override val view: View get() = throw UnsupportedOperationException()
    override val drawable: Drawable? get() = null
}
