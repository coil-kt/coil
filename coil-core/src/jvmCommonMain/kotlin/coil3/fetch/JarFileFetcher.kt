package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.util.MimeTypeMap
import coil3.util.extension
import okio.Path.Companion.toPath
import okio.openZip

internal class JarFileFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val path = uri.path.orEmpty()
        val delimiterIndex = path.indexOf('!')
        check(delimiterIndex != -1) { "Invalid jar:file URI: $uri" }

        val jarPath = path.substring(0, delimiterIndex).toPath()
        val filePath = path.substring(delimiterIndex + 1, path.length).toPath()

        return SourceFetchResult(
            source = ImageSource(filePath, options.fileSystem.openZip(jarPath)),
            mimeType = MimeTypeMap.getMimeTypeFromExtension(filePath.extension),
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return JarFileFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "jar:file"
        }
    }
}
