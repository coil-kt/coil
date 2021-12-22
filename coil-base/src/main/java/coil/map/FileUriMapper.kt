package coil.map

import android.content.ContentResolver
import android.net.Uri
import coil.request.Options
import coil.util.firstPathSegment
import coil.util.isAssetUri
import java.io.File

internal class FileUriMapper : Mapper<Uri, File> {

    override fun map(data: Uri, options: Options): File? {
        if (!isApplicable(data)) return null
        return File(data.path!!)
    }

    private fun isApplicable(data: Uri): Boolean {
        return !isAssetUri(data) &&
            data.scheme.let { it == null || it == ContentResolver.SCHEME_FILE } &&
            data.path.orEmpty().startsWith('/') && data.firstPathSegment != null
    }
}
