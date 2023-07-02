package coil

import coil.decode.BitmapFactoryDecoder
import coil.fetch.AssetUriFetcher
import coil.fetch.BitmapFetcher
import coil.fetch.ByteBufferFetcher
import coil.fetch.ContentUriFetcher
import coil.fetch.DrawableFetcher
import coil.fetch.HttpUriFetcher
import coil.fetch.PathFetcher
import coil.fetch.ResourceUriFetcher
import coil.key.PathKeyer
import coil.key.UriKeyer
import coil.map.FileUriMapper
import coil.map.HttpUrlMapper
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.map.StringMapper

internal actual fun ComponentRegistry.Builder.addPlatformComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(HttpUrlMapper())
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper())
        .add(ResourceIntMapper())
        // Keyers
        .add(UriKeyer())
        .add(PathKeyer(options.addLastModifiedToFileCacheKey))
        // Fetchers
        .add(HttpUriFetcher.Factory(options.httpClientLazy, options.diskCacheLazy, options.respectCacheHeaders))
        .add(PathFetcher.Factory())
        .add(AssetUriFetcher.Factory())
        .add(ContentUriFetcher.Factory())
        .add(ResourceUriFetcher.Factory())
        .add(DrawableFetcher.Factory())
        .add(BitmapFetcher.Factory())
        .add(ByteBufferFetcher.Factory())
        // Decoders
        .add(BitmapFactoryDecoder.Factory(options.bitmapFactoryMaxParallelism, options.bitmapFactoryExifOrientationPolicy))
}
