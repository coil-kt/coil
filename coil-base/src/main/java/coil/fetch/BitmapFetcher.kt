package coil.fetch

import android.content.Context
import android.graphics.Bitmap
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import coil.util.toDrawable

internal class BitmapFetcher(private val context: Context) : Fetcher<Bitmap> {

    override fun key(data: Bitmap): String? = null

    override suspend fun fetch(
        pool: BitmapPool,
        data: Bitmap,
        size: Size,
        options: Options
    ): FetchResult {
        return DrawableResult(
            drawable = data.toDrawable(context),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }
}
