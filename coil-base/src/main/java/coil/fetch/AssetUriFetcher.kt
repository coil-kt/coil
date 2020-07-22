package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import coil.util.firstPathSegment
import coil.util.getMimeTypeFromUrl
import okio.buffer
import okio.source

internal class AssetUriFetcher(private val context: Context) : Fetcher<Uri> {

    override fun handles(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_FILE && data.firstPathSegment == ASSET_FILE_PATH_ROOT
    }

    override fun key(data: Uri) = data.toString()

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
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
