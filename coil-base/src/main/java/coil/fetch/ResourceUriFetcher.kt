package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.webkit.MimeTypeMap
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.decode.Options
import coil.size.Size
import coil.util.getDrawableCompat
import coil.util.getMimeTypeFromUrl
import coil.util.getXmlDrawableCompat
import coil.util.isVector
import coil.util.nightMode
import coil.util.toDrawable
import okio.buffer
import okio.source

internal class ResourceUriFetcher(
    private val context: Context,
    private val drawableDecoder: DrawableDecoderService
) : Fetcher<Uri> {

    override fun handles(data: Uri) = data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE

    override fun key(data: Uri) = "$data-${context.resources.configuration.nightMode}"

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
        // Expected format: android.resource://example.package.name/12345678
        val packageName = data.authority?.takeIf { it.isNotBlank() } ?: throwInvalidUriException(data)
        val resId = data.pathSegments.lastOrNull()?.toIntOrNull() ?: throwInvalidUriException(data)

        val context = options.context
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val path = TypedValue().apply { resources.getValue(resId, this, true) }.string
        val entryName = path.substring(path.lastIndexOf('/'))
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromUrl(entryName)

        return if (mimeType == MIME_TYPE_XML) {
            // getDrawableCompat can only load resources that are in the current package.
            val drawable = if (packageName == context.packageName) {
                context.getDrawableCompat(resId)
            } else {
                context.getXmlDrawableCompat(resources, resId)
            }

            val isVector = drawable.isVector
            DrawableResult(
                drawable = if (isVector) {
                    drawableDecoder.convert(
                        drawable = drawable,
                        config = options.config,
                        size = size,
                        scale = options.scale,
                        allowInexactSize = options.allowInexactSize
                    ).toDrawable(context)
                } else {
                    drawable
                },
                isSampled = isVector,
                dataSource = DataSource.MEMORY
            )
        } else {
            SourceResult(
                source = resources.openRawResource(resId).source().buffer(),
                mimeType = mimeType,
                dataSource = DataSource.MEMORY
            )
        }
    }

    private fun throwInvalidUriException(data: Uri): Nothing {
        throw IllegalStateException("Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data")
    }

    companion object {
        private const val MIME_TYPE_XML = "text/xml"
    }
}
