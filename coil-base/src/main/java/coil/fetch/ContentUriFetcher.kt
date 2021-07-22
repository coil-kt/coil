package coil.fetch

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import okio.buffer
import okio.source
import java.io.InputStream

internal class ContentUriFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val context = options.context
        val inputStream = if (isContactPhotoUri(data)) {
            // Modified from ContactsContract.Contacts.openContactPhotoInputStream.
            val stream: InputStream? = context.contentResolver.openAssetFileDescriptor(data, "r")?.createInputStream()
            checkNotNull(stream) { "Unable to find a contact photo associated with '$data'." }
        } else {
            val stream: InputStream? = context.contentResolver.openInputStream(data)
            checkNotNull(stream) { "Unable to open '$data'." }
        }

        return SourceResult(
            source = ImageSource(inputStream.source().buffer(), context),
            mimeType = context.contentResolver.getType(data),
            dataSource = DataSource.DISK
        )
    }

    /** Contact photos are a special case of content uris that must be loaded using [ContentResolver.openAssetFileDescriptor]. */
    @VisibleForTesting
    internal fun isContactPhotoUri(data: Uri): Boolean {
        return data.authority == ContactsContract.AUTHORITY && data.lastPathSegment == Contacts.Photo.DISPLAY_PHOTO
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return ContentUriFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == ContentResolver.SCHEME_CONTENT
        }
    }
}
