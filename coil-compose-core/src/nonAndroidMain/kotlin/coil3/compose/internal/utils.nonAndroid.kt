package coil3.compose.internal

import coil3.request.ImageRequest

internal actual fun validateRequestProperties(request: ImageRequest) {
    require(request.target == null) { "request.target must be null." }
}
