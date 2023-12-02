package coil3.fetch

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.util.MimeTypeMap
import coil3.util.extension
import okio.Path

internal class PathFetcher(
    private val file: Path,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(file, options.fileSystem),
            mimeType = MimeTypeMap.getMimeTypeFromExtension(file.extension),
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Path> {
        override fun create(
            data: Path,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return PathFetcher(data, options)
        }
    }
}
