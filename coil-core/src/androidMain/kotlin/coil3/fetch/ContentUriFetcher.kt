package coil3.fetch

import android.content.ContentResolver
import android.content.ContentResolver.EXTRA_SIZE
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.res.AssetFileDescriptor
import android.graphics.Point
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import androidx.annotation.VisibleForTesting
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.BaseContentMetadata
import coil3.decode.ContentMetadata
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.decode.TypedContentMetadata
import coil3.pathSegments
import coil3.request.Options
import coil3.size.Dimension
import coil3.toAndroidUri
import okio.buffer
import okio.source

internal class ContentUriFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val androidUri = data.toAndroidUri()
        val contentResolver = options.context.contentResolver
        val metadata = if (SDK_INT >= 29 && isMusicThumbnailUri(data)) {
            val bundle = newMusicThumbnailSizeOptions()
            val mimeTypeFilter = "image/*"
            //noinspection Recycle: Automatically recycled after being decoded.
            val afd = contentResolver.openTypedAssetFile(androidUri, mimeTypeFilter, bundle, null)
            checkNotNull(afd) { "Unable to find a music thumbnail associated with '$androidUri'." }
            TypedContentMetadata(data, mimeTypeFilter, bundle, afd)
        } else {
            val afd = contentResolver.openAssetFileDescriptor(androidUri, "r")
            if (isContactPhotoUri(data)) {
                checkNotNull(afd) { "Unable to find a contact photo associated with '$androidUri'." }
            } else {
                checkNotNull(afd) { "Unable to open '$androidUri'." }
            }
            ContentMetadata(data, afd)
        }

        return SourceFetchResult(
            source = ImageSource(
                source = metadata.assetFileDescriptor.createInputStream().source().buffer(),
                fileSystem = options.fileSystem,
                metadata = metadata,
            ),
            mimeType = contentResolver.getType(androidUri),
            dataSource = DataSource.DISK,
        )
    }

    /**
     * Contact photos are a special case of content uris that must be loaded using
     * [ContentResolver.openAssetFileDescriptor] or [ContentResolver.openTypedAssetFile], as
     * opposed to [ContentResolver.openInputStream].
     */
    @VisibleForTesting
    internal fun isContactPhotoUri(data: Uri): Boolean {
        return data.authority == ContactsContract.AUTHORITY &&
            data.pathSegments.lastOrNull() == Contacts.Photo.DISPLAY_PHOTO
    }

    /**
     * Music thumbnails on API 29+ are a special case of content uris that must be loaded using
     * [ContentResolver.openTypedAssetFileDescriptor] with an `image/` MIME type.
     *
     * Example URIs: content://media/external/audio/albums/1961323289806133467 or
     * content://media/external/audio/media/27
     */
    @VisibleForTesting
    internal fun isMusicThumbnailUri(data: Uri): Boolean {
        if (data.authority != MediaStore.AUTHORITY) return false
        val segments = data.pathSegments
        val size = segments.size
        return size >= 3 && segments[size - 3] == "audio" &&
            (segments[size - 2] == "albums" || segments[size - 2] == "media")
    }

    private fun newMusicThumbnailSizeOptions(): Bundle? {
        val width = (options.size.width as? Dimension.Pixels)?.px ?: return null
        val height = (options.size.height as? Dimension.Pixels)?.px ?: return null
        return Bundle(1).apply { putParcelable(EXTRA_SIZE, Point(width, height)) }
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return ContentUriFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == SCHEME_CONTENT
        }
    }
}
