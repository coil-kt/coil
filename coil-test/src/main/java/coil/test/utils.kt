@file:JvmName("-Utils")

package coil.test

import android.graphics.drawable.Drawable
import coil.decode.DataSource
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.util.Collections

internal fun imageResultOf(drawable: Drawable, request: ImageRequest) = SuccessResult(
    drawable = drawable,
    request = request,
    dataSource = DataSource.MEMORY,
)

internal fun <T> List<T>.toImmutableList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> Collections.singletonList(first())
    else -> Collections.unmodifiableList(ArrayList(this))
}
