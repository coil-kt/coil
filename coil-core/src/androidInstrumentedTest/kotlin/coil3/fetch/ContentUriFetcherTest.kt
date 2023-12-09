package coil3.fetch

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri as AndroidUri
import android.os.Build.VERSION.SDK_INT
import android.provider.ContactsContract
import android.provider.ContactsContract.AUTHORITY
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
import android.provider.ContactsContract.Contacts.Photo.DISPLAY_PHOTO
import android.provider.ContactsContract.RawContacts
import android.provider.MediaStore
import androidx.core.net.toUri as toAndroidUri
import androidx.test.rule.GrantPermissionRule
import coil3.ImageLoader
import coil3.request.Options
import coil3.test.utils.assumeTrue
import coil3.test.utils.context
import coil3.toCoilUri
import coil3.toUri
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.sink
import okio.source
import org.junit.Rule
import org.junit.Test

class ContentUriFetcherTest {

    private val fetcherFactory = ContentUriFetcher.Factory()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(READ_CONTACTS, WRITE_CONTACTS)

    // Re-use the same contact across all tests. Must be created lazily.
    private val contactId by lazy(::createFakeContact)

    @Test
    fun contactsThumbnail() = runTest {
        // This test is flaky on API 30+.
        assumeTrue(SDK_INT < 30)

        val uri = "$SCHEME_CONTENT://$AUTHORITY/contacts/$contactId/$CONTENT_DIRECTORY".toUri()
        val fetcher = assertIs<ContentUriFetcher>(fetcherFactory.create(uri, Options(context), ImageLoader(context)))

        assertFalse(fetcher.isContactPhotoUri(uri))
        assertUriFetchesCorrectly(fetcher)
    }

    @Test
    fun contactsDisplayPhoto() = runTest {
        // This test is flaky on API 30+.
        assumeTrue(SDK_INT < 30)

        val uri = "$SCHEME_CONTENT://$AUTHORITY/contacts/$contactId/$DISPLAY_PHOTO".toUri()
        val fetcher = assertIs<ContentUriFetcher>(fetcherFactory.create(uri, Options(context), ImageLoader(context)))

        assertTrue(fetcher.isContactPhotoUri(uri))
        assertUriFetchesCorrectly(fetcher)
    }

    @Test
    fun musicThumbnail() {
        assumeTrue(SDK_INT >= 29)

        val uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, 1).toCoilUri()
        val fetcher = assertIs<ContentUriFetcher>(fetcherFactory.create(uri, Options(context), ImageLoader(context)))

        assertTrue(fetcher.isMusicThumbnailUri(uri))
    }

    private suspend fun assertUriFetchesCorrectly(fetcher: ContentUriFetcher) {
        val result = fetcher.fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }

    /** Create and insert a fake contact. Return its ID. */
    private fun createFakeContact(): Long {
        val values = ContentValues()
        values.put(RawContacts.ACCOUNT_TYPE, "com.google")
        values.put(RawContacts.ACCOUNT_NAME, "email")
        val uri = checkNotNull(context.contentResolver.insert(RawContacts.CONTENT_URI, values))
        val id = ContentUris.parseId(uri)

        values.clear()
        values.put(ContactsContract.Data.RAW_CONTACT_ID, id)
        values.put(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        values.put(StructuredName.DISPLAY_NAME, "John Smith $id")
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)

        values.clear()
        values.put(ContactsContract.Data.RAW_CONTACT_ID, id)
        values.put(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        values.put(Phone.NUMBER, "12345678910")
        values.put(Phone.TYPE, Phone.TYPE_MOBILE)
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)

        val contentUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, id)
        val photoUri = AndroidUri.withAppendedPath(contentUri, RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
        val fd = checkNotNull(context.contentResolver.openAssetFileDescriptor(photoUri, "rw"))

        fd.use {
            val source = context.assets.open("normal.jpg").source()
            val sink = it.createOutputStream().sink().buffer()
            source.use { sink.use { sink.writeAll(source) } }
        }

        // Wait for the display image to be parsed by the system.
        var attempts = 0
        while (attempts++ < 300 && !isContactParsed(id)) {
            Thread.sleep(100)
        }

        // Wait a little longer.
        Thread.sleep(1000)

        return id
    }

    private fun isContactParsed(id: Long): Boolean {
        return try {
            fun directoryExists(directory: String): Boolean {
                return context.contentResolver
                    .openAssetFileDescriptor("$SCHEME_CONTENT://$AUTHORITY/contacts/$id/$directory".toAndroidUri(), "r")
                    ?.createInputStream() != null
            }
            directoryExists(CONTENT_DIRECTORY) && directoryExists(DISPLAY_PHOTO)
        } catch (_: Throwable) {
            false
        }
    }
}
