package coil.fetch

import android.media.MediaDataSource
import androidx.annotation.RequiresApi
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer

@RequiresApi(23)
class MediaDataSourceFetcher(
    private val data: MediaDataSource,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val imageSource = ImageSource(
            source = MediaDataSourceOkioSource(data).buffer(),
            context = options.context,
            metadata = MediaSourceMetadata(data),
        )

        return SourceResult(
            source = imageSource,
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<MediaDataSource> {

        override fun create(
            data: MediaDataSource,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return MediaDataSourceFetcher(data, options)
        }
    }

    internal class MediaDataSourceOkioSource(
        private val mediaDataSource: MediaDataSource
    ) : Source {

        private var size = mediaDataSource.size
        private var position = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (position >= size) {
                // indicates EOF
                return -1
            }

            val sizeToRead = minOf(byteCount, size - position)
            val byteArray = ByteArray(sizeToRead.toInt())
            val readBytes = mediaDataSource.readAt(position, byteArray, 0, byteArray.size)

            position += readBytes
            sink.write(byteArray, 0, readBytes)

            return readBytes.toLong()
        }

        override fun timeout(): Timeout {
            return Timeout.NONE
        }

        override fun close() {
            mediaDataSource.close()
        }
    }

    @RequiresApi(23)
    class MediaSourceMetadata(val mediaDataSource: MediaDataSource) : ImageSource.Metadata()
}
