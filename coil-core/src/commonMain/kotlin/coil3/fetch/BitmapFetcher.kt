package coil3.fetch

import coil3.Bitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.request.Options

internal class BitmapFetcher(
    private val data: Bitmap,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return ImageFetchResult(
            image = data.asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Bitmap> {

        override fun create(data: Bitmap, options: Options, imageLoader: ImageLoader): Fetcher {
            return BitmapFetcher(data)
        }
    }
}
