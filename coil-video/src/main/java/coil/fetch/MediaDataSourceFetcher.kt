package coil.fetch

import android.media.MediaDataSource
import android.os.Build
import androidx.annotation.RequiresApi
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalCoilApi::class)
class MediaDataSourceFetcher(
    private val data: MediaDataSource,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val imageSource = ImageSource(
            source = MediaDataSourceOkIoSource(data).buffer(),
            context = options.context,
            metadata = MediaSourceMetadata(data),
        )

        return SourceResult(
            source = imageSource,
            mimeType = "video/", // TODO: How to get the mime type?
            dataSource = DataSource.MEMORY, // TODO: MEMORY or DISK?
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

    internal class MediaDataSourceOkIoSource(private val mediaDataSource: MediaDataSource) : Source {

        private var size = mediaDataSource.size
        private var position: Long = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (position >= size) {
                // indicates EOF
                return -1
            }

            val sizeToRead = when {
                position + byteCount > size -> (size - position)
                else -> byteCount
            }

            val byteArray = ByteArray(sizeToRead.toInt())
            val readBytes = mediaDataSource.readAt(position, byteArray, 0, byteArray.size)

            position += readBytes
            sink.write(byteArray, 0, readBytes)

            return readBytes.toLong()
        }

        override fun timeout(): Timeout {
            return Timeout()
        }

        override fun close() {
            mediaDataSource.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    class MediaSourceMetadata(val mediaDataSource: MediaDataSource) : ImageSource.Metadata()
}
