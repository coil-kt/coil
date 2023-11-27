package coil

import coil.decode.BitmapFactoryDecoder
import coil.fetch.AssetUriFetcher
import coil.fetch.BitmapFetcher
import coil.fetch.ContentUriFetcher
import coil.fetch.DrawableFetcher
import coil.fetch.ResourceUriFetcher
import coil.key.AndroidResourceUriKeyer
import coil.map.AndroidUriMapper
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.OneShotDisposable
import coil.request.lifecycle
import coil.request.requestManager
import coil.request.transitionFactory
import coil.target.Target
import coil.target.ViewTarget
import coil.transition.NoneTransition
import coil.transition.TransitionTarget
import coil.util.awaitStarted
import kotlinx.coroutines.Deferred

internal actual fun getDisposable(
    request: ImageRequest,
    job: Deferred<ImageResult>,
): Disposable {
    if (request.target is ViewTarget<*>) {
        return request.target.view.requestManager.getDisposable(job)
    } else {
        return OneShotDisposable(job)
    }
}

internal actual suspend fun awaitLifecycleStarted(
    request: ImageRequest,
) {
    request.lifecycle?.awaitStarted()
}

internal actual inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setDrawable: () -> Unit,
) {
    if (target !is TransitionTarget) {
        setDrawable()
        return
    }

    val transition = result.request.transitionFactory.create(target, result)
    if (transition is NoneTransition) {
        setDrawable()
        return
    }

    eventListener.transitionStart(result.request, transition)
    transition.transition()
    eventListener.transitionEnd(result.request, transition)
}

internal actual fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(AndroidUriMapper())
        .add(ResourceUriMapper())
        .add(ResourceIntMapper())
        // Keyers
        .add(AndroidResourceUriKeyer())
        // Fetchers
        .add(AssetUriFetcher.Factory())
        .add(ContentUriFetcher.Factory())
        .add(ResourceUriFetcher.Factory())
        .add(DrawableFetcher.Factory())
        .add(BitmapFetcher.Factory())
        // Decoders
        .add(
            BitmapFactoryDecoder.Factory(
                maxParallelism = options.bitmapFactoryMaxParallelism,
                exifOrientationPolicy = options.bitmapFactoryExifOrientationPolicy,
            ),
        )
}
