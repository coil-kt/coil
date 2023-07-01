package coil.fetch

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import coil.util.MimeTypeMap
import coil.util.extension
import okio.Path

internal class PathFetcher(
    private val data: Path,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(data, options.fileSystem),
            mimeType = MimeTypeMap.getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Path> {
        override fun create(data: Path, options: Options, imageLoader: ImageLoader): Fetcher {
            return PathFetcher(data, options)
        }
    }
}
