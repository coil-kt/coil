package coil.map

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.request.Options
import coil.util.ASSET_FILE_PATH_ROOT
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test

class FileUriMapperTest {

    private lateinit var context: Context
    private lateinit var mapper: FileUriMapper

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mapper = FileUriMapper()
    }

    @Test
    fun basic() {
        val uri = "$SCHEME_FILE:///path/to/file".toUri()
        assertEquals(File("/path/to/file"), mapper.map(uri, Options(context)))
    }

    @Test
    fun noScheme() {
        val uri = "/path/to/file".toUri()
        assertEquals(File("/path/to/file"), mapper.map(uri, Options(context)))
    }

    @Test
    fun doesNotHandleAssetUris() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/asset.jpg".toUri()
        assertNull(mapper.map(uri, Options(context)))
    }

    @Test
    fun doesNotHandleGenericString() {
        val uri = "generic_string".toUri()
        assertNull(mapper.map(uri, Options(context)))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1344 */
    @Test
    fun parsesPoundCharacterCorrectly() {
        val path = "/sdcard/fi#le.jpg"
        assertEquals(File(path), mapper.map(path.toUri(), Options(context)))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
    @Test
    fun ignoresAfterPathCorrectly() {
        val expected = File("/sdcard/file.jpg")
        val uri = Uri.parse("$SCHEME_FILE:///sdcard/file.jpg?query=value&query2=value#fragment")
        assertEquals(expected, mapper.map(uri, Options(context)))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
    @Test
    fun decodesEncodedPath() {
        val expected = File("/sdcard/Some File.jpg")
        val uri = Uri.parse("$SCHEME_FILE:///sdcard/Some%20File.jpg")
        assertEquals(expected, mapper.map(uri, Options(context)))
    }
}
