package coil.fetch

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.provider.ContactsContract
import android.provider.ContactsContract.AUTHORITY
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
import android.provider.ContactsContract.Contacts.Photo.DISPLAY_PHOTO
import android.provider.ContactsContract.RawContacts
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import coil.bitmap.BitmapPool
import coil.decode.Options
import coil.size.PixelSize
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentUriFetcherTest {

    private lateinit var context: Context
    private lateinit var fetcher: ContentUriFetcher
    private lateinit var pool: BitmapPool

    @get:Rule val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(READ_CONTACTS, WRITE_CONTACTS)

    // Re-use the same contact across all tests. Must be created lazily.
    private val contactId by lazy(::createFakeContact)

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        fetcher = ContentUriFetcher(context)
        pool = BitmapPool(0)
    }

    @Test
    fun contactsThumbnail() {
        // This test is flaky on API 30.
        assumeTrue(SDK_INT <= 30)

        val uri = "$SCHEME_CONTENT://$AUTHORITY/contacts/$contactId/$CONTENT_DIRECTORY".toUri()

        assertFalse(fetcher.isContactPhotoUri(uri))
        assertTrue(fetcher.handles(uri))
        assertUriFetchesCorrectly(uri)
    }

    @Test
    fun contactsDisplayPhoto() {
        // This test is flaky on API 30.
        assumeTrue(SDK_INT <= 30)

        val uri = "$SCHEME_CONTENT://$AUTHORITY/contacts/$contactId/$DISPLAY_PHOTO".toUri()

        assertTrue(fetcher.isContactPhotoUri(uri))
        assertTrue(fetcher.handles(uri))
        assertUriFetchesCorrectly(uri)
    }

    private fun assertUriFetchesCorrectly(uri: Uri) {
        assertTrue(fetcher.handles(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), Options(context))
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
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
        val photoUri = Uri.withAppendedPath(contentUri, RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
        val fd = checkNotNull(context.contentResolver.openAssetFileDescriptor(photoUri, "rw"))

        // AssetFileDescriptor does not implement Closable before API 19 so we can't use `use`.
        @Suppress("ConvertTryFinallyToUseCall")
        try {
            val source = context.assets.open("normal.jpg").source()
            val sink = fd.createOutputStream().sink().buffer()
            source.use { sink.use { sink.writeAll(source) } }
        } finally {
            fd.close()
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
                    .openAssetFileDescriptor("$SCHEME_CONTENT://$AUTHORITY/contacts/$id/$directory".toUri(), "r")
                    ?.createInputStream() != null
            }
            directoryExists(CONTENT_DIRECTORY) && directoryExists(DISPLAY_PHOTO)
        } catch (_: Throwable) {
            false
        }
    }
}
