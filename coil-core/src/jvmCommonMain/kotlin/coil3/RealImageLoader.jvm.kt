package coil3

import coil3.fetch.ByteBufferFetcher
import coil3.fetch.JarFileFetcher
import coil3.map.FileMapper

internal actual fun ComponentRegistry.Builder.addJvmComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(FileMapper())
        // Fetchers
        .add(JarFileFetcher.Factory())
        .add(ByteBufferFetcher.Factory())
}
