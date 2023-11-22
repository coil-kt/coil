package coil

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UriTest {

    @Test
    fun network() {
        val uri = "https://www.example.com/image.jpg?q=jpg#fragment".toUri()
        assertEquals("https", uri.scheme)
        assertEquals("www.example.com", uri.authority)
        assertEquals("/image.jpg", uri.path)
        assertEquals("q=jpg", uri.query)
        assertEquals("fragment", uri.fragment)
    }

    @Test
    fun relative() {
        val uri = "/test/relative/image.jpg#something".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("/test/relative/image.jpg", uri.path)
        assertNull(uri.query)
        assertEquals("something", uri.fragment)
    }

    @Test
    fun malformatted() {
        val uri = "#something:/test/relative/image.jpg".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("/test/relative/image.jpg", uri.path)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }
}
