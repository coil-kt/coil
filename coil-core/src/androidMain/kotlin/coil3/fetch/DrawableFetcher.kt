package coil3.fetch

import android.graphics.drawable.Drawable
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.util.DrawableUtils
import coil3.util.isVector
import coil3.util.toDrawable

internal class DrawableFetcher(
    private val data: Drawable,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val isVector = data.isVector
        return ImageFetchResult(
            image = if (isVector) {
                DrawableUtils.convertToBitmap(
                    drawable = data,
                    config = options.bitmapConfig,
                    size = options.size,
                    scale = options.scale,
                    allowInexactSize = options.allowInexactSize,
                ).toDrawable(options.context)
            } else {
                data
            }.asCoilImage(),
            isSampled = isVector,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Drawable> {

        override fun create(data: Drawable, options: Options, imageLoader: ImageLoader): Fetcher {
            return DrawableFetcher(data, options)
        }
    }
}
