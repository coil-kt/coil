package coil3.test.internal

import coil3.Image
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult

internal fun imageResultOf(
    image: Image,
    request: ImageRequest,
) = SuccessResult(
    image = image,
    request = request,
    dataSource = DataSource.MEMORY,
)

// Format: 0xALPHA_RED_GREEN_BLUE
internal const val Black = 0xFF_00_00_00.toInt()
