package coil3.svg.internal

import coil3.PlatformContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal expect suspend inline fun <T> runInterruptible(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline block: () -> T,
): T

internal const val MIME_TYPE_SVG = "image/svg+xml"
internal const val SVG_DEFAULT_SIZE = 512

// Use a default 2KB value for the SVG's size in memory.
internal const val SVG_SIZE_BYTES = 2048L

internal expect val PlatformContext.density: Float
