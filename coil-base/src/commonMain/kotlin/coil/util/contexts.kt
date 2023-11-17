package coil.util

import coil.PlatformContext

/** Return the global application context. */
internal expect val PlatformContext.application: PlatformContext

/** Return the default percent of the application's total memory to use for the memory cache. */
internal expect fun PlatformContext.defaultMemoryCacheSizePercent(): Double

/** Return the application's total memory in bytes. */
internal expect fun PlatformContext.totalAvailableMemoryBytes(): Long
