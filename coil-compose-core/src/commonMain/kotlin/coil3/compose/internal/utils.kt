package coil3.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.AsyncImagePainter.State
import coil3.compose.EqualityDelegate
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.NullRequestDataException
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size as CoilSize
import coil3.size.SizeResolver
import kotlin.math.roundToInt

/** Create an [ImageRequest] from the [model]. */
@Composable
@NonRestartableComposable
internal fun requestOf(model: Any?): ImageRequest {
    if (model is ImageRequest) {
        return model
    } else {
        val context = LocalPlatformContext.current
        return remember(context, model) {
            ImageRequest.Builder(context)
                .data(model)
                .build()
        }
    }
}

/** Create an [ImageRequest] with a not-null [SizeResolver] from the [model]. */
@Composable
@NonRestartableComposable
internal fun requestOfWithSizeResolver(
    model: Any?,
    contentScale: ContentScale,
): ImageRequest {
    if (model is ImageRequest && model.defined.sizeResolver != null) {
        return model
    }

    val sizeResolver = if (contentScale == ContentScale.None) {
        OriginalSizeResolver
    } else {
        remember { ConstraintsSizeResolver() }
    }

    if (model is ImageRequest) {
        return remember(model, sizeResolver) {
            model.newBuilder()
                .size(sizeResolver)
                .build()
        }
    } else {
        val context = LocalPlatformContext.current
        return remember(context, model, sizeResolver) {
            ImageRequest.Builder(context)
                .data(model)
                .size(sizeResolver)
                .build()
        }
    }
}

@Stable
internal fun transformOf(
    placeholder: Painter?,
    error: Painter?,
    fallback: Painter?,
): (State) -> State {
    return if (placeholder != null || error != null || fallback != null) {
        { state ->
            when (state) {
                is State.Loading -> {
                    if (placeholder != null) state.copy(painter = placeholder) else state
                }
                is State.Error -> if (state.result.throwable is NullRequestDataException) {
                    if (fallback != null) state.copy(painter = fallback) else state
                } else {
                    if (error != null) state.copy(painter = error) else state
                }
                else -> state
            }
        }
    } else {
        DefaultTransform
    }
}

@Stable
internal fun onStateOf(
    onLoading: ((State.Loading) -> Unit)?,
    onSuccess: ((State.Success) -> Unit)?,
    onError: ((State.Error) -> Unit)?,
): ((State) -> Unit)? {
    return if (onLoading != null || onSuccess != null || onError != null) {
        { state ->
            when (state) {
                is State.Loading -> onLoading?.invoke(state)
                is State.Success -> onSuccess?.invoke(state)
                is State.Error -> onError?.invoke(state)
                is State.Empty -> {}
            }
        }
    } else {
        null
    }
}

/** Wrap [AsyncImage]'s unstable arguments to make them stable. */
@Stable
internal class AsyncImageState(
    val model: Any?,
    val modelEqualityDelegate: EqualityDelegate,
    val imageLoader: ImageLoader,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is AsyncImageState &&
            modelEqualityDelegate.equals(model, other.model) &&
            imageLoader == other.imageLoader
    }

    override fun hashCode(): Int {
        var result = modelEqualityDelegate.hashCode(model)
        result = 31 * result + imageLoader.hashCode()
        return result
    }
}

@Stable
internal fun Modifier.contentDescription(contentDescription: String?): Modifier {
    if (contentDescription != null) {
        return semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        return this
    }
}

@Stable
internal fun ContentScale.toScale() = when (this) {
    ContentScale.Fit, ContentScale.Inside -> Scale.FIT
    else -> Scale.FILL
}

@Stable
internal fun Constraints.toSizeOrNull(): CoilSize? {
    if (isZero) {
        return null
    } else {
        val width = if (hasBoundedWidth) Dimension(maxWidth) else Dimension.Undefined
        val height = if (hasBoundedHeight) Dimension(maxHeight) else Dimension.Undefined
        return CoilSize(width, height)
    }
}

internal fun Constraints.constrainWidth(width: Float) =
    width.coerceIn(minWidth.toFloat(), maxWidth.toFloat())

internal fun Constraints.constrainHeight(height: Float) =
    height.coerceIn(minHeight.toFloat(), maxHeight.toFloat())

internal inline fun Float.takeOrElse(block: () -> Float) = if (isFinite()) this else block()

internal fun Size.toIntSize() = IntSize(width.roundToInt(), height.roundToInt())

internal val Size.isPositive get() = width >= 0.5 && height >= 0.5

internal val ZeroConstraints = Constraints.fixed(0, 0)

internal val OriginalSizeResolver = SizeResolver(CoilSize.ORIGINAL)
