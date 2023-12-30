package coil3.map

import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.toUri
import coil3.util.SCHEME_FILE
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Path.Companion.toPath

class PathMapperTest : RobolectricTest() {

    private val mapper = PathMapper()

    @Test
    fun basic() {
        val expected = "$SCHEME_FILE:///path/to/file".toUri()
        val actual = mapper.map("/path/to/file".toPath(), Options(context))
        assertEquals(expected, actual)
    }

//    @Test
//    fun noScheme() {
//        val uri = "/path/to/file".toUri()
//        assertEquals(File("/path/to/file"), mapper.map(uri, Options(context)))
//    }
//
//    @Test
//    fun doesNotHandleAssetUris() {
//        val uri = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/asset.jpg".toUri()
//        assertNull(mapper.map(uri, Options(context)))
//    }
//
//    @Test
//    fun doesNotHandleGenericString() {
//        val uri = "generic_string".toUri()
//        assertNull(mapper.map(uri, Options(context)))
//    }
//
//    /** Regression test: https://github.com/coil-kt/coil/issues/1344 */
//    @Test
//    fun parsesPoundCharacterCorrectly() {
//        val path = "/sdcard/fi#le.jpg"
//        assertEquals(File(path), mapper.map(path.toUri(), Options(context)))
//    }
//
//    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
//    @Test
//    fun ignoresAfterPathCorrectly() {
//        val expected = File("/sdcard/file.jpg")
//        val uri = "$SCHEME_FILE:///sdcard/file.jpg?query=value&query2=value#fragment".toUri()
//        assertEquals(expected, mapper.map(uri, Options(context)))
//    }
//
//    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
//    @Test
//    fun decodesEncodedPath() {
//        val expected = File("/sdcard/Some File.jpg")
//        val uri = "$SCHEME_FILE:///sdcard/Some%20File.jpg".toUri()
//        assertEquals(expected, mapper.map(uri, Options(context)))
//    }

    companion object {
        const val ASSET_FILE_PATH_ROOT = "android_asset"
    }
}
