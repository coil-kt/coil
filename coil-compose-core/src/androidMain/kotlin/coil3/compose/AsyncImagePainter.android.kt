package coil3.compose

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.BitmapImage
import coil3.Image
import coil3.PlatformContext
import coil3.compose.internal.CrossfadePainter
import coil3.request.SuccessResult
import coil3.request.transitionFactory
import coil3.transition.CrossfadeTransition
import coil3.transition.TransitionTarget
import com.google.accompanist.drawablepainter.DrawablePainter

internal actual fun Image.toPainter(
    context: PlatformContext,
    filterQuality: FilterQuality,
): Painter {
    return when (this) {
        is BitmapImage -> BitmapPainter(
            image = bitmap.asImageBitmap(),
            filterQuality = filterQuality,
        )
        else -> DrawablePainter(asDrawable(context.resources).mutate())
    }
}

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
    val transition = result.request.transitionFactory.create(fakeTransitionTarget, result)
    if (transition is CrossfadeTransition) {
        return CrossfadePainter(
            start = previous.painter.takeIf { previous is AsyncImagePainter.State.Loading },
            end = current.painter,
            contentScale = contentScale,
            durationMillis = transition.durationMillis,
            fadeStart = result !is SuccessResult || !result.isPlaceholderCached,
            preferExactIntrinsicSize = transition.preferExactIntrinsicSize,
        )
    } else {
        return null
    }
}

private val fakeTransitionTarget = object : TransitionTarget {
    override val view get() = throw UnsupportedOperationException()
    override val drawable: Drawable? get() = null
}
