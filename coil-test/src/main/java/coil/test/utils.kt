@file:JvmName("-Utils")

package coil.test

import android.graphics.drawable.Drawable
import coil.decode.DataSource
import coil.request.ImageRequest
import coil.request.SuccessResult

internal fun imageResultOf(drawable: Drawable, request: ImageRequest) = SuccessResult(
    drawable = drawable,
    request = request,
    dataSource = DataSource.MEMORY,
)
