package coil3.test

import coil3.test.FakeImageLoaderEngine.RequestTransformer

internal actual fun defaultRequestTransformer(): RequestTransformer {
    return RequestTransformer { request -> request }
}
