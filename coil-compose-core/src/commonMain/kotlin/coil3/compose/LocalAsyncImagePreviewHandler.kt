package coil3.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.Canvas
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlin.jvm.JvmField

@ExperimentalCoilApi
val LocalAsyncImagePreviewHandler = staticCompositionLocalOf { AsyncImagePreviewHandler.Default }

/**
 * Controls what [AsyncImage], [SubcomposeAsyncImage], and [AsyncImagePainter] renders when
 * [LocalInspectionMode] is true.
 */
@ExperimentalCoilApi
fun interface AsyncImagePreviewHandler {

    fun handle(
        imageLoader: ImageLoader,
        request: ImageRequest,
        toPainter: Image.() -> Painter,
    ): AsyncImagePainter.State

    companion object {
        @JvmField val Default = AsyncImagePreviewHandler { _, request, _ ->
            AsyncImagePainter.State.Success(
                painter = ColorPainter(Color(0x88888888)),
                result = SuccessResult(
                    image = FakeImage,
                    request = request,
                ),
            )
        }
    }
}

private val FakeImage = object : Image {
    override val size: Long get() = 0L
    override val width: Int get() = -1
    override val height: Int get() = -1
    override val shareable: Boolean get() = true
    override fun draw(canvas: Canvas) {}
}
