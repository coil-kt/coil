package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.AssetMetadata
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.pathSegments
import coil3.request.Options
import coil3.util.MimeTypeMap
import coil3.util.isAssetUri
import okio.buffer
import okio.source

internal class AssetUriFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val path = data.pathSegments.drop(1).joinToString("/")

        return SourceFetchResult(
            source = ImageSource(
                source = options.context.assets.open(path).source().buffer(),
                fileSystem = options.fileSystem,
                metadata = AssetMetadata(path),
            ),
            mimeType = MimeTypeMap.getMimeTypeFromUrl(path),
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isAssetUri(data)) return null
            return AssetUriFetcher(data, options)
        }
    }
}
