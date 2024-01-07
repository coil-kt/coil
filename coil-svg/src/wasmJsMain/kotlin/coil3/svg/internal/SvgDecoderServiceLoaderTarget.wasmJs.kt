package coil3.svg.internal

import coil3.annotation.InternalCoilApi

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
@InternalCoilApi
@Deprecated("", level = DeprecationLevel.HIDDEN)
val initHook: Any = ServiceLoaderComponentRegistry.register(SvgDecoderServiceLoaderTarget())
