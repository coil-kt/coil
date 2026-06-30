package coil3.decode

import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants.SEEK_SET
import androidx.annotation.DrawableRes
import coil3.Uri

/**
 * Metadata containing the [filePath] of an Android asset.
 */
class AssetMetadata(
    val filePath: String,
) : ImageSource.Metadata()

/**
 * Abstract superclass for metadata relating to the asset file descriptor obtained from a
 * `content` URI.
 */
abstract class BaseContentMetadata : ImageSource.Metadata() {
    abstract val assetFileDescriptor: AssetFileDescriptor
    val seekable by lazy {
        try {
            Os.lseek(
                assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset,
                SEEK_SET,
            )
            true
        } catch (_: ErrnoException) {
            false
        }
    }
}

/**
 * Metadata containing the [uri] and associated [assetFileDescriptor] of a `content` URI
 * that can be opened without special considerations.
 */
class ContentMetadata(
    val uri: Uri,
    override val assetFileDescriptor: AssetFileDescriptor,
) : BaseContentMetadata()

/**
 * Metadata containing the [uri] and associated [assetFileDescriptor] of a `content` URI
 * that must be opened with [android.content.ContentResolver.openTypedAssetFileDescriptor] (as
 * opposed to [android.content.ContentResolver.openAssetFileDescriptor]), while also passing the
 * [mimeTypeFilter] and [opts] (if present) to the content resolver.
 */
class TypedContentMetadata(
    val uri: Uri,
    val mimeTypeFilter: String,
    val opts: Bundle?,
    override val assetFileDescriptor: AssetFileDescriptor,
) : BaseContentMetadata()

/**
 * Metadata containing the [packageName], [resId], and [density] of an Android resource.
 */
class ResourceMetadata(
    val packageName: String,
    @get:DrawableRes @param:DrawableRes val resId: Int,
    val density: Int,
) : ImageSource.Metadata()
