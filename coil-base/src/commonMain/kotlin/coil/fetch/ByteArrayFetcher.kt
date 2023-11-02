package coil.fetch

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import okio.Buffer

internal class ByteArrayFetcher(
    private val byteArray: ByteArray,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(Buffer().apply { write(byteArray) }),
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
