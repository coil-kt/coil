package coil.test

import coil.test.FakeImageLoaderEngine.RequestTransformer

internal actual fun defaultRequestTransformer(): RequestTransformer {
    return RequestTransformer { request -> request }
}
