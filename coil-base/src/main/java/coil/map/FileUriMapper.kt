package coil.map

import android.content.ContentResolver
import android.net.Uri
import coil.fetch.AssetUriFetcher
import coil.request.Options
import coil.util.firstPathSegment
import java.io.File

internal class FileUriMapper : Mapper<Uri, File> {

    override fun map(data: Uri, options: Options): File? {
        if (!isApplicable(data)) return null
        return File(checkNotNull(data.path))
    }

    private fun isApplicable(data: Uri): Boolean {
        if (isAssetUri(data)) return false
        return data.scheme.let { it == null || it == ContentResolver.SCHEME_FILE } &&
            data.path.orEmpty().startsWith('/') && data.firstPathSegment != null
    }

    private fun isAssetUri(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_FILE &&
            data.firstPathSegment == AssetUriFetcher.ASSET_FILE_PATH_ROOT
    }
}
