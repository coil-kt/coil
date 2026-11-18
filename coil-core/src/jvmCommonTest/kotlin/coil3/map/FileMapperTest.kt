package coil3.map

import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.util.SCHEME_FILE
import java.io.File
import kotlin.test.assertEquals
import org.junit.Test

class FileMapperTest : RobolectricTest() {
    private val mapper = FileMapper()

    @Test
    fun basic() {
        val file = File("/path/to/file")
        val uri = mapper.map(file, Options(context))
        assertEquals(SCHEME_FILE, uri.scheme)
        assertEquals(file.path, uri.path)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1344 */
    @Test
    fun parsesPoundCharacterCorrectly() {
        val file = File("/sdcard/fi#le.jpg")
        assertEquals(file.path, mapper.map(file, Options(context)).path)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1513 */
    @Test
    fun decodesEncodedPath() {
        val file = File("/sdcard/Some+File.jpg")
        val uri = mapper.map(file, Options(context))
        assertEquals(SCHEME_FILE, uri.scheme)
        assertEquals(file.path, uri.path)
    }
}
