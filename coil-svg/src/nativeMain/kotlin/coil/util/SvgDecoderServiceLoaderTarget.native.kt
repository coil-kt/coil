package coil.util

import coil3.annotation.InternalCoilApi
import coil3.util.ServiceLoaderComponentRegistry
import coil3.util.SvgDecoderServiceLoaderTarget

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
@InternalCoilApi
@Deprecated("", level = DeprecationLevel.HIDDEN)
val initHook: Any = ServiceLoaderComponentRegistry.register(SvgDecoderServiceLoaderTarget())
