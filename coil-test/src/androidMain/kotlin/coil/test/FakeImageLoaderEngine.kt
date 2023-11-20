package coil.test

import coil.request.transitionFactory
import coil.test.FakeImageLoaderEngine.RequestTransformer
import coil.transition.Transition

internal actual fun defaultRequestTransformer(): RequestTransformer {
    return RequestTransformer { request ->
        request.newBuilder()
            .transitionFactory(Transition.Factory.NONE)
            .build()
    }
}
