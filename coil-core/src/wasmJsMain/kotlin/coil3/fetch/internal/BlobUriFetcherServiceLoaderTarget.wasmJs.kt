package coil3.fetch.internal

import coil3.annotation.InternalCoilApi
import coil3.util.ServiceLoaderComponentRegistry

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
@InternalCoilApi
@Deprecated("", level = DeprecationLevel.HIDDEN)
val initBlobUriFetcherHook: Any =
    ServiceLoaderComponentRegistry.register(BlobUriFetcherServiceLoaderTarget())
