package coil3.svg.internal

import coil3.PlatformContext

internal actual val PlatformContext.density: Float
    get() = resources.displayMetrics.density
