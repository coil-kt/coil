package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import coil.decode.DataSource
import coil.decode.Options
import coil.util.firstPathSegment
import coil.util.getMimeTypeFromUrl
import okio.buffer
import okio.source

internal class AssetUriFetcher(private val context: Context) : Fetcher<Uri> {

    override fun handles(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_FILE && data.firstPathSegment == ASSET_FILE_PATH_ROOT
    }

    override fun cacheKey(data: Uri) = data.toString()

    override suspend fun fetch(data: Uri, options: Options): FetchResult {
        val path = data.pathSegments.drop(1).joinToString("/")

        return SourceResult(
            source = context.assets.open(path).source().buffer(),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(path),
            dataSource = DataSource.DISK
        )
    }

    companion object {
        const val ASSET_FILE_PATH_ROOT = "android_asset"
    }
}
