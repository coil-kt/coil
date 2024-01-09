package coil3

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

    @Test
    fun complex() {
        val uri = ("https://images.unsplash.com/photo-1550939810-cb345b2f4ad7?ixlib=rb-1.2.1&q=80" +
            "&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max&ixid=eyJhcHBfaWQiOjU4MjM5fQ").toUri()
        assertEquals("https", uri.scheme)
        assertEquals("images.unsplash.com", uri.authority)
        assertEquals("/photo-1550939810-cb345b2f4ad7", uri.path)
        assertEquals("ixlib=rb-1.2.1&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max&ixid=eyJhcHBfaWQiOjU4MjM5fQ", uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun encoded() {
        val string = "https://example.com/%E4%B8%8A%E6%B5%B7%2B%E4%B8%AD%E5%9C%8B"
        val uri = string.toUri()
        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.authority)
        assertEquals("/上海+中國", uri.path)
        assertNull(uri.query)
        assertNull(uri.fragment)
        assertEquals(string, uri.toString())
    }

    @Test
    fun encodedMalformed() {
        val string = "https://example.com/%E4%B8%8A%E6%B5%B7%2B%E4%B8%AD%E5%9C%8B%6"
        val uri = string.toUri()
        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.authority)
        assertEquals("/上海+中國%6", uri.path)
        assertNull(uri.query)
        assertNull(uri.fragment)
        assertEquals(string, uri.toString())
    }

    @Test
    fun skipsEmptyPathSegments() {
        val uri = "file:///test///image.jpg".toUri()
        assertEquals("file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("/test///image.jpg", uri.path)
        assertNull(uri.query)
        assertNull(uri.fragment)
        assertEquals(listOf("test", "image.jpg"), uri.pathSegments)
    }
}
