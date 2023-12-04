package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.util.MimeTypeMap
import coil3.util.extension
import coil3.util.isFileUri
import okio.Path.Companion.toPath

internal class FileUriFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val path = checkNotNull(uri.path) { "path == null" }.toPath()
        return SourceFetchResult(
            source = ImageSource(path, options.fileSystem),
            mimeType = MimeTypeMap.getMimeTypeFromExtension(path.extension),
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isFileUri(data)) return null
            return FileUriFetcher(data, options)
        }
    }
}
