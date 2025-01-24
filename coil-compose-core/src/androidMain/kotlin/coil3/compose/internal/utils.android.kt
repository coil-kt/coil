package coil3.compose.internal

import coil3.request.ImageRequest
import coil3.request.lifecycle

internal actual fun validateRequestProperties(request: ImageRequest) {
    require(request.target == null) { "request.target must be null." }
    require(request.lifecycle == null) { "request.lifecycle must be null." }
}
