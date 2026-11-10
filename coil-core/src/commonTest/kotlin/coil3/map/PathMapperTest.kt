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
        val file = "/path/to/file".toPath()
        val uri = "$SCHEME_FILE:/path/to/file".toUri()
        assertEquals(uri, mapper.map(file, Options(context)))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1344 */
    @Test
    fun parsesPoundCharacterCorrectly() {
        val path = "/sdcard/fi#le.jpg"
        val file = "/sdcard/fi#le.jpg".toPath()
        assertEquals(path, mapper.map(file, Options(context)).path)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
    @Test
    fun decodesEncodedPath() {
        val path = "/sdcard/Some+File.jpg"
        val uri = "$SCHEME_FILE:$path".toUri()
        val file = path.toPath()
        assertEquals(uri, mapper.map(file, Options(context)))
    }
}
