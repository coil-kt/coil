package coil3.fetch

import coil3.ImageLoader
import coil3.decode.ByteBufferMetadata
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import java.nio.ByteBuffer
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer

internal class ByteBufferFetcher(
    private val data: ByteBuffer,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                source = data.asSource().buffer(),
                fileSystem = options.fileSystem,
                metadata = ByteBufferMetadata(data),
            ),
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

internal fun ByteBuffer.asSource() = object : Source {
    private val buffer = this@asSource.slice()
    private val len = buffer.capacity()

    override fun close() = Unit

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (buffer.position() == len) return -1
        val pos = buffer.position()
        val newLimit = (pos + byteCount).toInt().coerceAtMost(len)
        buffer.limit(newLimit)
        return sink.write(buffer).toLong()
    }

    override fun timeout() = Timeout.NONE
}
