package coil.map

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.request.Options
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
