package coil3.fetch

import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.request.Options
import coil3.util.toDrawable

internal class BitmapFetcher(
    private val data: Bitmap,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return ImageFetchResult(
            image = data.toDrawable(options.context).asCoilImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Bitmap> {

        override fun create(data: Bitmap, options: Options, imageLoader: ImageLoader): Fetcher {
            return BitmapFetcher(data, options)
        }
    }
}
