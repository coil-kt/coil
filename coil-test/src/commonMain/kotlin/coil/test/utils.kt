package coil.test

import coil.Image
import coil.decode.DataSource
import coil.request.ImageRequest
import coil.request.SuccessResult

internal fun imageResultOf(
    image: Image,
    request: ImageRequest,
) = SuccessResult(
    image = image,
    request = request,
    dataSource = DataSource.MEMORY,
)
