package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.collection.arraySetOf
import androidx.core.net.toFile
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import okio.buffer
import okio.source

internal class UriFetcher(
    private val context: Context
) : Fetcher<Uri> {

    companion object {
        private const val ASSET_FILE_PATH_SEGMENT = "android_asset"

        private val SUPPORTED_SCHEMES = arraySetOf(
            ContentResolver.SCHEME_ANDROID_RESOURCE,
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE
        )
    }

    override fun handles(data: Uri) = SUPPORTED_SCHEMES.contains(data.scheme)

    override fun key(data: Uri): String = if (data.scheme == ContentResolver.SCHEME_FILE) {
        "$data:${data.toFile().lastModified()}"
    } else {
        data.toString()
    }

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
        val assetFileName = extractAssetFileName(data)
        val inputStream = if (assetFileName != null) {
            context.assets.open(assetFileName)
        } else {
            checkNotNull(context.contentResolver.openInputStream(data))
        }

        return SourceResult(
            source = inputStream.source().buffer(),
            mimeType = context.contentResolver.getType(data),
            dataSource = DataSource.DISK
        )
    }

    /** Return the asset's filename if [uri] must be handled by [AssetManager]. Else, return null. */
    @VisibleForTesting
    internal fun extractAssetFileName(uri: Uri): String? {
        if (uri.scheme != ContentResolver.SCHEME_FILE) {
            return null
        }

        val segments = uri.pathSegments
        return if (segments.count() == 2 &&
            segments[0] == ASSET_FILE_PATH_SEGMENT &&
            segments[1].isNotBlank()) {
            segments[1]
        } else {
            null
        }
    }
}
