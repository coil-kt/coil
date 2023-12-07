package coil3.fetch

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import okio.Buffer

internal class ByteArrayFetcher(
    private val byteArray: ByteArray,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(byteArray) },
                fileSystem = options.fileSystem,
            ),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ByteArray> {
        override fun create(
            data: ByteArray,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ByteArrayFetcher(data, options)
        }
    }
}
