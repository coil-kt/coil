package coil.fetch

import android.content.ContentResolver
import android.content.ContentResolver.EXTRA_SIZE
import android.content.ContentResolver.SCHEME_CONTENT
import android.graphics.Point
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.decode.ContentMetadata
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import coil.size.Dimension
import okio.buffer
import okio.source

internal class ContentUriFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val contentResolver = options.context.contentResolver
        val inputStream = if (isContactPhotoUri(data)) {
            // Modified from ContactsContract.Contacts.openContactPhotoInputStream.
            val stream = contentResolver
                .openAssetFileDescriptor(data, "r")
                ?.createInputStream()
            checkNotNull(stream) { "Unable to find a contact photo associated with '$data'." }
        } else if (SDK_INT >= 29 && isMusicThumbnailUri(data)) {
            val bundle = newMusicThumbnailSizeOptions()
            val stream = contentResolver
                .openTypedAssetFile(data, "image/*", bundle, null)
                ?.createInputStream()
            checkNotNull(stream) { "Unable to find a music thumbnail associated with '$data'." }
        } else {
            val stream = contentResolver.openInputStream(data)
            checkNotNull(stream) { "Unable to open '$data'." }
        }

        return SourceResult(
            source = ImageSource(
                source = inputStream.source().buffer(),
                context = options.context,
                metadata = ContentMetadata(data)
            ),
            mimeType = contentResolver.getType(data),
            dataSource = DataSource.DISK
        )
    }

    /**
     * Contact photos are a special case of content uris that must be loaded using
     * [ContentResolver.openAssetFileDescriptor] or [ContentResolver.openTypedAssetFile].
     */
    @VisibleForTesting
    internal fun isContactPhotoUri(data: Uri): Boolean {
        return data.authority == ContactsContract.AUTHORITY &&
            data.lastPathSegment == Contacts.Photo.DISPLAY_PHOTO
    }

    /**
     * Music thumbnails on API 29+ are a special case of content uris that must be loaded using
     * [ContentResolver.openAssetFileDescriptor] or [ContentResolver.openTypedAssetFile].
     *
     * Example URI: content://media/external/audio/albums/1961323289806133467
     */
    @VisibleForTesting
    internal fun isMusicThumbnailUri(data: Uri): Boolean {
        if (data.authority != MediaStore.AUTHORITY) return false
        val segments = data.pathSegments
        val size = segments.size
        return size >= 3 && segments[size - 3] == "audio" && segments[size - 2] == "albums"
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

        private fun isApplicable(data: Uri) = data.scheme == SCHEME_CONTENT
    }
}
