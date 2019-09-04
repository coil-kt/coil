package coil.fetch

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.webkit.MimeTypeMap
import androidx.annotation.DrawableRes
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.decode.Options
import coil.size.Size
import coil.util.getDrawableCompat
import coil.util.getMimeTypeFromUrl
import okio.buffer
import okio.source

internal class ResourceFetcher(
    private val context: Context,
    private val drawableDecoder: DrawableDecoderService
) : Fetcher<@DrawableRes Int> {

    companion object {
        private const val MIME_TYPE_XML = "text/xml"
    }

    override fun handles(@DrawableRes data: Int) = try {
        context.resources.getResourceEntryName(data) != null
    } catch (e: Resources.NotFoundException) {
        false
    }

    override fun key(@DrawableRes data: Int) = "res:$data"

    @SuppressLint("ResourceType")
    override suspend fun fetch(
        pool: BitmapPool,
        @DrawableRes data: Int,
        size: Size,
        options: Options
    ): FetchResult {
        val path = TypedValue().apply { context.resources.getValue(data, this, true) }.string
        val entryName = path.substring(path.lastIndexOf('/'))
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(entryName)

        return if (mimeType == MIME_TYPE_XML) {
            DrawableResult(
                drawable = drawableDecoder.convertIfNecessary(
                    drawable = context.getDrawableCompat(data),
                    size = size,
                    config = options.config
                ),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        } else {
            SourceResult(
                source = context.resources.openRawResource(data).source().buffer(),
                mimeType = mimeType,
                dataSource = DataSource.MEMORY
            )
        }
    }
}
