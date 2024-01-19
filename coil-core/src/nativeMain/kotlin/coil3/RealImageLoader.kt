package coil3

import coil3.map.NSURLMapper

internal actual fun ComponentRegistry.Builder.addAppleComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(NSURLMapper())
}
