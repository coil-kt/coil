package coil.fetch

import android.graphics.Bitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.Options
import coil.util.toDrawable

internal class BitmapFetcher(
    private val data: Bitmap,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return DrawableResult(
            drawable = data.toDrawable(options.context),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Bitmap> {

        override fun create(data: Bitmap, options: Options, imageLoader: ImageLoader): Fetcher {
            return BitmapFetcher(data, options)
        }
    }
}
