package coil.fetch

import android.content.Context
import android.graphics.drawable.Drawable
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.decode.Options
import coil.size.Size
import coil.util.isVector
import coil.util.toDrawable

internal class DrawableFetcher(
    private val context: Context,
    private val drawableDecoder: DrawableDecoderService
) : Fetcher<Drawable> {

    override fun key(data: Drawable): String? = null

    override suspend fun fetch(
        pool: BitmapPool,
        data: Drawable,
        size: Size,
        options: Options
    ): FetchResult {
        return DrawableResult(
            drawable = if (data.isVector()) {
                drawableDecoder.convert(data, size, options.config).toDrawable(context)
            } else {
                data
            },
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }
}
