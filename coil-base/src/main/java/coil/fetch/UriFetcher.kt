package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.collection.arraySetOf
import androidx.core.net.toFile
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import coil.util.Utils
import coil.util.getDrawableCompat
import okio.buffer
import okio.source

internal class UriFetcher(
    private val context: Context
) : Fetcher<Uri> {

    companion object {

        private const val ASSET_FILE_PATH_SEGMENT = "android_asset"
        /**
         * [Resources.getIdentifier] documents that it will return 0 and
         * that 0 is not a valid resouce id.
         */
        private const val ANDROID_PACKAGE_NAME = "android"
        private const val MISSING_RESOURCE_ID = 0
        // android.resource://<package_name>/<type>/<name>.
        private const val NAME_URI_PATH_SEGMENTS = 2
        private const val TYPE_PATH_SEGMENT_INDEX = 0
        private const val NAME_PATH_SEGMENT_INDEX = 1
        // android.resource://<package_name>/<resource_id>
        private const val ID_PATH_SEGMENTS = 1
        private const val RESOURCE_ID_SEGMENT_INDEX = 0

        private val SUPPORTED_SCHEMES = arraySetOf(
            ContentResolver.SCHEME_ANDROID_RESOURCE,
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE
        )
    }

    override fun handles(data: Uri) = SUPPORTED_SCHEMES.contains(data.scheme)

    override fun key(data: Uri): String = if (data.scheme == ContentResolver.SCHEME_FILE) {
        "$data:${data.toFile().lastModified()}"
    } else {
        data.toString()
    }

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
        if (data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
            data.authority?.let { packageName ->
                val targetContext = findContextForPackage(context, data, packageName)
                val resourceId = extractResourceId(targetContext, data)
                val drawable = targetContext.getDrawableCompat(resourceId)
                val inputStream = Utils.getInputStreamFromBitmap(Utils.getBitmapFromDrawable(drawable, size))
                return SourceResult(
                    source = inputStream.source().buffer(),
                    mimeType = targetContext.contentResolver.getType(data),
                    dataSource = DataSource.DISK)
            }
        }

        val assetPath = extractAssetPath(data)
        val inputStream = if (assetPath != null) {
            context.assets.open(assetPath)
        } else {
            checkNotNull(context.contentResolver.openInputStream(data))
        }

        return SourceResult(
            source = inputStream.source().buffer(),
            mimeType = context.contentResolver.getType(data),
            dataSource = DataSource.DISK
        )
    }

    /** Return the asset's path if [uri] must be handled by [AssetManager]. Else, return null. */
    @VisibleForTesting
    internal fun extractAssetPath(uri: Uri): String? {
        if (uri.scheme != ContentResolver.SCHEME_FILE) {
            return null
        }

        val segments = uri.pathSegments
        if (segments.count() < 2 || segments[0] != ASSET_FILE_PATH_SEGMENT) {
            return null
        }

        val path = segments
            .drop(1)
            .joinToString("/")

        if (path.isBlank()) {
            return null
        }

        return path
    }

    /** Return the resource ID from Uri. Else, return null. */
    @VisibleForTesting
    internal fun extractResourceId(context: Context, source: Uri): Int {
        val segments = source.pathSegments
        return when {
            segments.count() == NAME_URI_PATH_SEGMENTS -> findResourceIdFromTypeAndNameResourceUri(context, source)
            segments.count() == ID_PATH_SEGMENTS -> findResourceIdFromUri(source)
            else -> throw IllegalArgumentException("Failed to find resource id for: $source")
        }
    }

    /** Return context for source Uri. */
    @VisibleForTesting
    internal fun findContextForPackage(mContext: Context, source: Uri, packageName: String): Context {
        if (packageName == mContext.packageName) {
            return mContext
        }

        try {
            return mContext.createPackageContext(packageName, /*flags=*/ 0)
        } catch (e: PackageManager.NameNotFoundException) {
            // The parent APK holds the correct context if the resource is located in a split
            if (packageName.contains(mContext.packageName)) {
                return mContext
            }
            throw PackageManager.NameNotFoundException(
                "Failed to find target package on device for : $source")
        }
    }

    // android.resource://com.android.camera2/mipmap/logo_camera_color
    @DrawableRes
    @VisibleForTesting
    internal fun findResourceIdFromTypeAndNameResourceUri(context: Context, source: Uri): Int {
        val segments = source.pathSegments
        val packageName = source.authority
        if (segments.isNotEmpty() && segments.count() == 2) {
            val typeName = segments[TYPE_PATH_SEGMENT_INDEX]
            val resourceName = segments[NAME_PATH_SEGMENT_INDEX]
            var result = context.resources.getIdentifier(resourceName, typeName, packageName)
            if (result == MISSING_RESOURCE_ID) {
                result = Resources.getSystem().getIdentifier(resourceName, typeName, ANDROID_PACKAGE_NAME)
            }
            if (result == MISSING_RESOURCE_ID) {
                throw IllegalArgumentException("Failed to find name resource id for: $source")
            }
            return result
        } else {
            throw IllegalArgumentException("Failed to find resource or unrecognized Uri format for: $source")
        }
    }

    // android.resource://com.android.camera2/123456
    @DrawableRes
    @VisibleForTesting
    internal fun findResourceIdFromUri(source: Uri): Int {
        val segments = source.pathSegments
        if (segments.isNotEmpty()) {
            return segments[RESOURCE_ID_SEGMENT_INDEX].toInt()
        } else {
            throw IllegalArgumentException("Failed to find resource id for: $source")
        }
    }
}
