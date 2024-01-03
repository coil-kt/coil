package coil3.util

import coil3.annotation.InternalCoilApi
import java.util.ServiceLoader

@InternalCoilApi
actual object ServiceLoaderComponentRegistry {
    actual val fetchers = FetcherServiceLoaderTarget::class.java.let {
        ServiceLoader.load(it, it.classLoader).toList().toImmutableList()
    }
    actual val decoders = DecoderServiceLoaderTarget::class.java.let {
        ServiceLoader.load(it, it.classLoader).toList().toImmutableList()
    }
}
