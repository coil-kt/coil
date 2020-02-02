package coil.map

import android.content.ContentResolver.SCHEME_FILE
import androidx.core.net.toUri
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
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
    fun basic() {
        val uri = "$SCHEME_FILE:///path/to/file".toUri()

        assertTrue(mapper.handles(uri))
        assertEquals(File("/path/to/file"), mapper.map(uri))
    }

    @Test
    fun doesNotHandleAssetUris() {
        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/asset.jpg".toUri()

        assertFalse(mapper.handles(uri))
    }
}
