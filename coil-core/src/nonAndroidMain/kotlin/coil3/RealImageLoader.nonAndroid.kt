package coil3

import coil3.decode.SkiaImageDecoder
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.OneShotDisposable
import coil3.target.Target
import kotlinx.coroutines.Deferred

internal actual fun skipCreatingDisposable(
    request: ImageRequest,
) = true

internal actual fun getDisposable(
    request: ImageRequest,
    job: Deferred<ImageResult>,
): Disposable = OneShotDisposable(job)

internal actual inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setDrawable: () -> Unit,
) = setDrawable()

internal actual fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Decoders
        .add(SkiaImageDecoder.Factory())
}
