package coil.test

import android.graphics.drawable.Drawable
import coil.asCoilImage
import coil.request.transitionFactory
import coil.test.FakeImageLoaderEngine.RequestTransformer
import coil.transition.Transition

/**
 * Create a new [FakeImageLoaderEngine] that returns [drawable] for all requests.
 */
@JvmName("create")
fun FakeImageLoaderEngine(drawable: Drawable): FakeImageLoaderEngine {
    return FakeImageLoaderEngine.Builder()
        .default(drawable.asCoilImage())
        .build()
}

fun FakeImageLoaderEngine.Builder.intercept(
    data: Any,
    drawable: Drawable,
) = intercept(data, drawable.asCoilImage())

fun FakeImageLoaderEngine.Builder.intercept(
    predicate: (data: Any) -> Boolean,
    drawable: Drawable,
) = intercept(predicate, drawable.asCoilImage())

fun FakeImageLoaderEngine.Builder.default(
    drawable: Drawable,
) = default(drawable.asCoilImage())

internal actual fun defaultRequestTransformer(): RequestTransformer {
    return RequestTransformer { request ->
        request.newBuilder()
            .transitionFactory(Transition.Factory.NONE)
            .build()
    }
}
