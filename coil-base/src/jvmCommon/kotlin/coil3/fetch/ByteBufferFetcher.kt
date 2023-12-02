package coil3.fetch

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import java.nio.ByteBuffer
import okio.Buffer

internal class ByteBufferFetcher(
    private val data: ByteBuffer,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val source = try {
            Buffer().apply { write(data) }
        } finally {
            // Reset the position so we can read the byte buffer again.
            data.position(0)
        }
        return SourceFetchResult(
            source = ImageSource(source, options.fileSystem),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ByteBuffer> {

        override fun create(
            data: ByteBuffer,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return ByteBufferFetcher(data, options)
        }
    }
}
