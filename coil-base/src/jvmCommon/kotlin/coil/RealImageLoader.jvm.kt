package coil

import coil.fetch.ByteBufferFetcher
import coil.map.FileMapper

internal actual fun ComponentRegistry.Builder.addJvmComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(FileMapper())
        // Fetchers
        .add(ByteBufferFetcher.Factory())
}
