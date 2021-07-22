package coil.fetch

import android.content.ContentResolver
import android.net.Uri
import android.util.TypedValue
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import coil.util.DrawableUtils
import coil.util.getDrawableCompat
import coil.util.getMimeTypeFromUrl
import coil.util.getXmlDrawableCompat
import coil.util.isVector
import coil.util.toDrawable
import okio.buffer
import okio.source

internal class ResourceUriFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
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
                    DrawableUtils.convertToBitmap(
                        drawable = drawable,
                        config = options.config,
                        size = options.size,
                        scale = options.scale,
                        allowInexactSize = options.allowInexactSize
                    ).toDrawable(context)
                } else {
                    drawable
                },
                isSampled = isVector,
                dataSource = DataSource.DISK
            )
        } else {
            SourceResult(
                source = ImageSource(
                    source = resources.openRawResource(resId).source().buffer(),
                    context = context
                ),
                mimeType = mimeType,
                dataSource = DataSource.DISK
            )
        }
    }

    private fun throwInvalidUriException(data: Uri): Nothing {
        throw IllegalStateException("Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data")
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return ResourceUriFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE
        }
    }

    companion object {
        private const val MIME_TYPE_XML = "text/xml"
    }
}
