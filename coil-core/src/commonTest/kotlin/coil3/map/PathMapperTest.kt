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
}
