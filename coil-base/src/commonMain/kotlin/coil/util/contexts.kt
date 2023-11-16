package coil.util

import coil.Context

/** Return the global application context. */
internal expect val Context.application: Context

/** Return the default percent of the application's total memory to use for the memory cache. */
internal expect fun Context.defaultMemoryCacheSizePercent(): Double

/** Return the application's total memory in bytes. */
internal expect fun Context.totalAvailableMemoryBytes(): Long
