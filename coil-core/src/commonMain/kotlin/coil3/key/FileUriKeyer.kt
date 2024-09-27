package coil3.key

import coil3.Uri
import coil3.filePath
import coil3.request.Options
import coil3.request.addLastModifiedToFileCacheKey
import coil3.util.isFileUri
import okio.Path.Companion.toPath

internal class FileUriKeyer : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String? {
        if (isFileUri(data) && options.addLastModifiedToFileCacheKey) {
            val path = data.filePath
            if (path != null) {
                val timestamp = options.fileSystem.metadata(path.toPath()).lastModifiedAtMillis
                return "$data-$timestamp"
            }
        }

        // Fall back to the standard UriKeyer.
        return null
    }
}
