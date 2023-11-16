package coil

import coil.decode.SkiaImageDecoder
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.OneShotDisposable
import coil.target.Target
import kotlinx.coroutines.Deferred

internal actual fun getDisposable(
    request: ImageRequest,
    job: Deferred<ImageResult>,
): Disposable = OneShotDisposable(job)

internal actual suspend fun awaitLifecycleStarted(
    request: ImageRequest,
) { /* Do nothing. */ }

internal actual inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setDrawable: () -> Unit,
) = setDrawable()

internal actual fun ComponentRegistry.Builder.addPlatformComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Decoders
        .add(SkiaImageDecoder.Factory())
}
