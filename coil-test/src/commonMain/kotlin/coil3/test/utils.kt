package coil3.test

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
