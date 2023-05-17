package coil.fetch

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.Options
import coil.util.DrawableUtils
import coil.util.isVector
import coil.util.toDrawable

internal class DrawableFetcher(
    private val data: Drawable,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val isVector = data.isVector
        return DrawableResult(
            drawable = if (isVector) {
                DrawableUtils.convertToBitmap(
                    drawable = data,
                    config = options.config,
                    size = options.size,
                    scale = options.scale,
                    allowInexactSize = options.allowInexactSize
                ).toDrawable(options.context)
            } else {
                data
            },
            isSampled = isVector,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Drawable> {

        override fun create(data: Drawable, options: Options, imageLoader: ImageLoader): Fetcher {
            return DrawableFetcher(data, options)
        }
    }
}
