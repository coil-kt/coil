@file:Suppress("DEPRECATION", "unused")

package coil.compose

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import coil.request.ImageRequest

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and
 * renders the result.
 *
 * This is a lower-level API than [AsyncImage] and may not work as expected in all situations.
 * Notably, it will not finish loading if [AsyncImagePainter.onDraw] is not called, which can occur
 * for composables that don't have a fixed size (e.g. [LazyColumn]). It's recommended to use
 * [AsyncImage] unless you need a reference to a [Painter].
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn
 *  into the destination.
 */
@Composable
fun rememberAsyncImagePainter(
    model: Any?,
    filterQuality: FilterQuality = DefaultFilterQuality,
): AsyncImagePainter = rememberAsyncImagePainter(model, LocalImageLoader.current, filterQuality)
