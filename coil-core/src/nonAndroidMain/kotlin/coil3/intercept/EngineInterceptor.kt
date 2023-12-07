package coil3.intercept

import coil3.EventListener
import coil3.Image
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.util.Logger

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
