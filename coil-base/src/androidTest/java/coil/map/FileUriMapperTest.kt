package coil.map

import android.content.ContentResolver
import android.net.Uri
import coil.fetch.AssetUriFetcher
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileUriMapperTest {

    private lateinit var mapper: FileUriMapper

    @Before
    fun before() {
        mapper = FileUriMapper()
    }

    @Test
    fun basicFileUri() {
        val uri = Uri.parse("${ContentResolver.SCHEME_FILE}:///path/to/file")

        assertTrue(mapper.handles(uri))
        assertEquals(File("/path/to/file"), mapper.map(uri))
    }

    @Test
    fun doesNotHandleAssetUris() {
        val uri = Uri.parse("${ContentResolver.SCHEME_FILE}:///${AssetUriFetcher.ASSET_FILE_PATH_ROOT}/asset.jpg")

        assertFalse(mapper.handles(uri))
    }
}
