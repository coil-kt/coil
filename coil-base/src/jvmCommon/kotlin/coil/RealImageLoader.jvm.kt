package coil

import coil.fetch.ByteBufferFetcher

internal actual fun ComponentRegistry.Builder.addJvmComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        .add(ByteBufferFetcher.Factory())
}
