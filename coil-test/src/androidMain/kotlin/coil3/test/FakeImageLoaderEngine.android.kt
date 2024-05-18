package coil3.test

import android.graphics.drawable.Drawable
import coil3.asImage
import coil3.request.transitionFactory
import coil3.test.FakeImageLoaderEngine.RequestTransformer
import coil3.transition.Transition

/**
 * Create a new [FakeImageLoaderEngine] that returns [drawable] for all requests.
 */
fun FakeImageLoaderEngine(drawable: Drawable): FakeImageLoaderEngine {
    return FakeImageLoaderEngine.Builder()
        .default(drawable)
        .build()
}

fun FakeImageLoaderEngine.Builder.intercept(
    data: Any,
    drawable: Drawable,
) = intercept(data, drawable.asImage())

fun FakeImageLoaderEngine.Builder.intercept(
    predicate: (data: Any) -> Boolean,
    drawable: Drawable,
) = intercept(predicate, drawable.asImage())

fun FakeImageLoaderEngine.Builder.default(
    drawable: Drawable,
) = default(drawable.asImage())

internal actual fun defaultRequestTransformer(): RequestTransformer {
    return RequestTransformer { request ->
        request.newBuilder()
            .transitionFactory(Transition.Factory.NONE)
            .build()
    }
}
