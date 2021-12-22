package coil.fetch

import android.net.Uri
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.AssetMetadata
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import coil.util.getMimeTypeFromUrl
import coil.util.isAssetUri
import okio.buffer
import okio.source

internal class AssetUriFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val path = data.pathSegments.drop(1).joinToString("/")

        return SourceResult(
            source = ImageSource(
                source = options.context.assets.open(path).source().buffer(),
                context = options.context,
                metadata = AssetMetadata(data.lastPathSegment!!)
            ),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(path),
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isAssetUri(data)) return null
            return AssetUriFetcher(data, options)
        }
    }
}
