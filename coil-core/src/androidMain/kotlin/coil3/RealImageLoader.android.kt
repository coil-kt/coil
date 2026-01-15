package coil3

import android.os.Build.VERSION.SDK_INT
import coil3.decode.BitmapFactoryDecoder
import coil3.decode.ExifOrientationStrategy.Companion.RESPECT_PERFORMANCE
import coil3.decode.StaticImageDecoder
import coil3.fetch.AssetUriFetcher
import coil3.fetch.ContentUriFetcher
import coil3.fetch.DrawableFetcher
import coil3.fetch.ResourceUriFetcher
import coil3.key.AndroidResourceUriKeyer
import coil3.map.AndroidUriMapper
import coil3.map.ResourceIntMapper
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.OneShotDisposable
import coil3.request.lifecycle
import coil3.request.requestManager
import coil3.request.transitionFactory
import coil3.size.ViewSizeResolver
import coil3.target.Target
import coil3.target.ViewTarget
import coil3.transition.NoneTransition
import coil3.transition.TransitionTarget
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Semaphore

internal actual fun needsExecuteOnMainDispatcher(
    request: ImageRequest,
): Boolean {
    // Don't dispatch to the main thread unless we have to interact with a view or a lifecycle.
    return request.target is ViewTarget<*> ||
        request.sizeResolver is ViewSizeResolver<*> ||
        request.lifecycle != null
}

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

internal actual inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setImage: () -> Unit,
) {
    if (target !is TransitionTarget) {
        setImage()
        return
    }

    val transition = result.request.transitionFactory.create(target, result)
    if (transition is NoneTransition) {
        setImage()
        return
    }

    eventListener.transitionStart(result.request, transition)
    transition.transition()
    eventListener.transitionEnd(result.request, transition)
}

internal actual fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder = apply {
    // Mappers
    add(AndroidUriMapper())
    add(ResourceIntMapper())

    // Keyers
    add(AndroidResourceUriKeyer())

    // Fetchers
    add(AssetUriFetcher.Factory())
    add(ContentUriFetcher.Factory())
    add(ResourceUriFetcher.Factory())
    add(DrawableFetcher.Factory())

    // Decoders
    val parallelismLock = Semaphore(options.bitmapFactoryMaxParallelism)
    if (options.enableStaticImageDecoder()) {
        add(
            StaticImageDecoder.Factory(
                parallelismLock = parallelismLock,
            ),
        )
    }
    add(
        BitmapFactoryDecoder.Factory(
            parallelismLock = parallelismLock,
            exifOrientationStrategy = options.bitmapFactoryExifOrientationStrategy,
        ),
    )
}

private fun RealImageLoader.Options.enableStaticImageDecoder(): Boolean {
    // Require API 29 for ImageDecoder support as API 28 has framework bugs:
    // https://github.com/element-hq/element-android/pull/7184
    return SDK_INT >= 29 && imageDecoderEnabled && bitmapFactoryExifOrientationStrategy == RESPECT_PERFORMANCE
}
