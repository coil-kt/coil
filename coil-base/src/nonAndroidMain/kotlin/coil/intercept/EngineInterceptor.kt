package coil.intercept

import coil.EventListener
import coil.Image
import coil.intercept.EngineInterceptor.ExecuteResult
import coil.request.ImageRequest
import coil.request.Options
import coil.util.Logger

internal actual suspend fun transform(
    result: ExecuteResult,
    request: ImageRequest,
    options: Options,
    eventListener: EventListener,
    logger: Logger?,
) = result

internal actual fun prepareToDraw(
    image: Image,
) { /* Do nothing. */ }
