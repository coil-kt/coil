package coil.map

import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import coil.request.Options
import coil.util.firstPathSegment
import coil.util.isAssetUri
import java.io.File

internal class FileUriMapper : Mapper<Uri, File> {

    override fun map(data: Uri, options: Options): File? {
        if (!isApplicable(data)) return null
        if (data.scheme == SCHEME_FILE) {
            return data.path?.let(::File)
        } else {
            // If the scheme is not "file", it's null, representing a literal path on disk.
            // Assume the entire input, regardless of any reserved characters, is valid.
            return File(data.toString())
        }
    }

    private fun isApplicable(data: Uri): Boolean {
        return !isAssetUri(data) &&
            data.scheme.let { it == null || it == SCHEME_FILE } &&
            data.path.orEmpty().startsWith('/') && data.firstPathSegment != null
    }
}
