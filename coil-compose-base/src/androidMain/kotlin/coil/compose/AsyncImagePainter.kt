package coil.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.Image
import coil.drawable
import coil.request.SuccessResult
import coil.request.transitionFactory
import coil.transition.CrossfadeTransition
import coil.transition.TransitionTarget
import com.google.accompanist.drawablepainter.DrawablePainter

internal actual fun Image.toPainter(
    filterQuality: FilterQuality,
): Painter {
    return when (val drawable = drawable) {
        is BitmapDrawable -> BitmapPainter(
            image = drawable.bitmap.asImageBitmap(),
            filterQuality = filterQuality,
        )
        else -> DrawablePainter(drawable.mutate())
    }
}

/** Create and return a [CrossfadePainter] if requested. */
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
