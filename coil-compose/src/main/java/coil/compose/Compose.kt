package coil.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.ImageLoader
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Precision
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

val LocalImageLoader = staticCompositionLocalOf<ImageLoader?> { null }

object CoilPainterDefaults {

    @Composable
    fun defaultImageLoader(): ImageLoader {
        return LocalImageLoader.current ?: LocalContext.current.imageLoader
    }
}

@Composable
fun rememberCoilPainter(
    request: Any?,
    imageLoader: ImageLoader = CoilPainterDefaults.defaultImageLoader(),
    shouldRefetchOnSizeChange: ShouldRefetchOnSizeChange = ShouldRefetchOnSizeChange { _, _ -> false },
    requestBuilder: (ImageRequest.Builder.(size: IntSize) -> ImageRequest.Builder)? = null,
    fadeIn: Boolean = false,
    fadeInDurationMs: Int = LoadPainterDefaults.FadeInTransitionDuration,
    @DrawableRes previewPlaceholder: Int = 0,
): LoadPainter<Any> {
    // Remember and update a CoilLoader
    val context = LocalContext.current
    val coilLoader = remember {
        CoilLoader(context, imageLoader, requestBuilder)
    }.apply {
        this.context = context
        this.imageLoader = imageLoader
        this.requestBuilder = requestBuilder
    }
    return rememberLoadPainter(
        loader = coilLoader,
        request = checkData(request),
        shouldRefetchOnSizeChange = shouldRefetchOnSizeChange,
        fadeIn = fadeIn,
        fadeInDurationMs = fadeInDurationMs,
        previewPlaceholder = previewPlaceholder,
    )
}

internal class CoilLoader(
    context: Context,
    imageLoader: ImageLoader,
    requestBuilder: (ImageRequest.Builder.(size: IntSize) -> ImageRequest.Builder)?,
) : Loader<Any> {
    var context by mutableStateOf(context)
    var imageLoader by mutableStateOf(imageLoader)
    var requestBuilder by mutableStateOf(requestBuilder)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun load(request: Any, size: IntSize): Flow<ImageLoadState> = channelFlow {
        val baseRequest = when (request) {
            // If we've been given an ImageRequest instance, use it...
            is ImageRequest -> request.newBuilder()
            // Otherwise we construct a request from the data
            else -> {
                ImageRequest.Builder(context)
                    .data(request)
                    // We force in-exact precision as AUTOMATIC only works when used from views.
                    // INEXACT is correct as we can scale the result appropriately.
                    .precision(Precision.INEXACT)
            }
        }.apply {
            // Apply the request builder
            requestBuilder?.invoke(this, size)
        }.target(
            onStart = { placeholder ->
                // We need to send blocking, to ensure that Loading is sent
                // before the execute result below.
                trySendBlocking(
                    ImageLoadState.Loading(
                        placeholder = placeholder?.let(::DrawablePainter),
                        request = request
                    )
                )
            }
        ).build()

        val sizedRequest = when {
            // If the request has a size resolver set we just execute the request as-is
            baseRequest.defined.sizeResolver != null -> baseRequest
            // If the size contains an unspecified sized dimension, we don't specify a size
            // in the Coil request
            size.width < 0 || size.height < 0 -> baseRequest
            // If we have a non-zero size, we can modify the request to include the size
            size.width > 0 && size.height > 0 -> {
                baseRequest.newBuilder()
                    .size(size.width, size.height)
                    .build()
            }
            // Otherwise we have a zero size, so no point executing a request
            else -> {
                if (!isClosedForSend) send(ImageLoadState.Empty)
                return@channelFlow
            }
        }

        val result = imageLoader.execute(sizedRequest).toResult(request)

        if (!isClosedForSend) send(result)
    }
}

private fun ImageResult.toResult(request: Any): ImageLoadState = when (this) {
    is SuccessResult -> {
        ImageLoadState.Success(
            result = DrawablePainter(drawable),
            request = request,
            source = metadata.dataSource
        )
    }
    is ErrorResult -> {
        ImageLoadState.Error(
            result = drawable?.let(::DrawablePainter),
            request = request,
            throwable = throwable
        )
    }
}

private fun checkData(data: Any?): Any? {
    when (data) {
        is Drawable -> {
            throw IllegalArgumentException(
                "Unsupported type: Drawable." +
                    " If you wish to load a drawable, pass in the resource ID."
            )
        }
        is ImageBitmap -> {
            throw IllegalArgumentException(
                "Unsupported type: ImageBitmap." +
                    " If you wish to display this ImageBitmap, use androidx.compose.foundation.Image()"
            )
        }
        is ImageVector -> {
            throw IllegalArgumentException(
                "Unsupported type: ImageVector." +
                    " If you wish to display this ImageVector, use androidx.compose.foundation.Image()"
            )
        }
        is Painter -> {
            throw IllegalArgumentException(
                "Unsupported type: Painter." +
                    " If you wish to draw this Painter, use androidx.compose.foundation.Image()"
            )
        }
    }
    return data
}
