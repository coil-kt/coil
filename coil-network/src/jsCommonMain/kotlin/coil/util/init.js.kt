package coil.util

import coil3.annotation.InternalCoilApi
import coil3.util.ServiceLoaderComponentRegistry
import coil3.util.NetworkFetcherServiceLoaderTarget

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class, ExperimentalJsExport::class)
@EagerInitialization
@JsExport
@InternalCoilApi
@Deprecated("", level = DeprecationLevel.HIDDEN)
val initHook: Any = ServiceLoaderComponentRegistry.register(NetworkFetcherServiceLoaderTarget())
