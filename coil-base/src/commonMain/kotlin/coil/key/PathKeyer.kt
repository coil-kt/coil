package coil.key

import coil.request.Options
import okio.Path

internal class PathKeyer(
    private val addLastModifiedToFileCacheKey: Boolean,
) : Keyer<Path> {

    override fun key(data: Path, options: Options): String {
        return if (addLastModifiedToFileCacheKey) {
            val lastModifiedAtMillis = options.fileSystem.metadata(data).lastModifiedAtMillis
            "$data:$lastModifiedAtMillis"
        } else {
            "$data"
        }
    }
}
