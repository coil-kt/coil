package coil.fetch

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import coil.util.firstPathSegment
import coil.util.getMimeTypeFromUrl
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
                context = options.context
            ),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(path),
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return AssetUriFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == ContentResolver.SCHEME_FILE &&
                data.firstPathSegment == ASSET_FILE_PATH_ROOT
        }
    }

    companion object {
        const val ASSET_FILE_PATH_ROOT = "android_asset"
    }
}
