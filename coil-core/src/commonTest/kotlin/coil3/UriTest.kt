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
        assertEquals(listOf("image.jpg"), uri.pathSegments)
        assertEquals("q=jpg", uri.query)
        assertEquals("fragment", uri.fragment)
    }

    @Test
    fun relative() {
        val uri = "/test/relative/image.jpg#something".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("/test/relative/image.jpg", uri.path)
        assertEquals(listOf("test", "relative", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("something", uri.fragment)
    }

    @Test
    fun malformed() {
        val uri = "#something:/test/relative/image.jpg".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertNull(uri.path)
        assertEquals(listOf(), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("something:/test/relative/image.jpg", uri.fragment)
    }

    @Test
    fun veryMalformed() {
        val uri = "/#02dkfj;anc%%2".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("/", uri.path)
        assertEquals(listOf(), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("02dkfj;anc%%2", uri.fragment)
    }

    @Test
    fun complex() {
        val uri = ("https://images.unsplash.com/photo-1550939810-cb345b2f4ad7?ixlib=rb-1.2.1&q=80" +
            "&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max&ixid=eyJhcHBfaWQiOjU4MjM5fQ").toUri()
        assertEquals("https", uri.scheme)
        assertEquals("images.unsplash.com", uri.authority)
        assertEquals("/photo-1550939810-cb345b2f4ad7", uri.path)
        assertEquals(listOf("photo-1550939810-cb345b2f4ad7"), uri.pathSegments)
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
        assertEquals(listOf("上海+中國"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
        assertEquals(string, uri.toString())
    }

    @Test
    fun encodedSingle() {
        val string = "https://example.com/something%20"
        val uri = string.toUri()
        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.authority)
        assertEquals("/something ", uri.path)
        assertEquals(listOf("something "), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
        assertEquals(string, uri.toString())
    }

    @Test
    fun encodedMalformed() {
        val string = "https://example.com/%E4%B8%8A%E6%B5%B7%2B%E4%B8%AD%E5%9C%8B%"
        val uri = string.toUri()
        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.authority)
        assertEquals("/上海+中國%", uri.path)
        assertEquals(listOf("上海+中國%"), uri.pathSegments)
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
        assertEquals(listOf("test", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun noPath() {
        val uri = "https://example.com?a=b#c".toUri()
        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.authority)
        assertEquals(null, uri.path)
        assertEquals(listOf(), uri.pathSegments)
        assertEquals("a=b", uri.query)
        assertEquals("c", uri.fragment)
    }
}
