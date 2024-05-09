package coil3.util

import coil3.annotation.InternalCoilApi
import java.util.ServiceLoader

@InternalCoilApi
actual object ServiceLoaderComponentRegistry {
    // This code is written intentionally so R8 can optimize it:
    // https://github.com/Kotlin/kotlinx.coroutines/issues/1231
    actual val fetchers by lazy {
        ServiceLoader.load(
            FetcherServiceLoaderTarget::class.java,
            FetcherServiceLoaderTarget::class.java.classLoader,
        ).iterator().asSequence().toList().toImmutableList()
    }
    actual val decoders by lazy {
        ServiceLoader.load(
            DecoderServiceLoaderTarget::class.java,
            DecoderServiceLoaderTarget::class.java.classLoader,
        ).iterator().asSequence().toList().toImmutableList()
    }
}
