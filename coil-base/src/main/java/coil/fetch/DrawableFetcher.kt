package coil.fetch

import android.graphics.drawable.Drawable
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.decode.Options
import coil.size.Size

internal class DrawableFetcher(
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
            drawable = drawableDecoder.convertIfNecessary(data, size, options.config),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }
}
