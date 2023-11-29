package coil.key

import coil.Uri
import coil.request.Options
import coil.util.isFileUri
import okio.Path.Companion.toPath

class FileUriKeyer(
    private val addLastModifiedToFileCacheKey: Boolean,
) : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String? {
        if (addLastModifiedToFileCacheKey && isFileUri(data)) {
            val path = data.path
            if (path != null) {
                val timestamp = options.fileSystem.metadata(path.toPath()).lastModifiedAtMillis
                return "$data:$timestamp"
            }
        }
        return null
    }
}
