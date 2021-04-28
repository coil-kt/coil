package coil.fetch

import android.graphics.Bitmap
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import coil.util.toDrawable

internal class BitmapFetcher : Fetcher<Bitmap> {

    override fun key(data: Bitmap): String? = null

    override suspend fun fetch(
        data: Bitmap,
        size: Size,
        options: Options
    ): FetchResult {
        return DrawableResult(
            drawable = data.toDrawable(options.context),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }
}
