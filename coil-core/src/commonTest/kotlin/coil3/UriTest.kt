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
        assertEquals("/image.jpg", uri.filePath)
        assertEquals(listOf("image.jpg"), uri.pathSegments)
        assertEquals("q=jpg", uri.query)
        assertEquals("fragment", uri.fragment)
    }

    @Test
    fun absolute() {
        val uri = "/test/absolute/image.jpg#something".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("/test/absolute/image.jpg", uri.path)
        assertEquals("/test/absolute/image.jpg", uri.filePath)
        assertEquals(listOf("test", "absolute", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("something", uri.fragment)
    }

    @Test
    fun relative() {
        val uri = "test/relative/image.jpg#something".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("test/relative/image.jpg", uri.path)
        assertEquals("test/relative/image.jpg", uri.filePath)
        assertEquals(listOf("test", "relative", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("something", uri.fragment)
    }

    @Test
    fun absoluteWithFileScheme() {
        val uri = "file:///test/absolute/image.jpg#something".toUri()
        assertEquals("file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("/test/absolute/image.jpg", uri.path)
        assertEquals("/test/absolute/image.jpg", uri.filePath)
        assertEquals(listOf("test", "absolute", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("something", uri.fragment)
    }

    @Test
    fun relativeWithFileScheme() {
        val uri = "file://test/relative/image.jpg#something".toUri()
        assertEquals("file", uri.scheme)
        // This looks wrong, but is consistent with the URI specification.
        // The authority is always the first path after the scheme and "://".
        assertEquals("test", uri.authority)
        assertEquals("/relative/image.jpg", uri.path)
        assertEquals("/relative/image.jpg", uri.filePath)
        assertEquals(listOf("relative", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertEquals("something", uri.fragment)
    }

    @Test
    fun absoluteFileUriWithoutAuthority() {
        val uri = "file:/test/image.jpg".toUri()
        assertEquals("file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("/test/image.jpg", uri.path)
        assertEquals("/test/image.jpg", uri.filePath)
        assertEquals(listOf("test", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun relativeFileUriWithoutAuthority() {
        val uri = "file:test/image.jpg".toUri()
        assertEquals("file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("test/image.jpg", uri.path)
        assertEquals("test/image.jpg", uri.filePath)
        assertEquals(listOf("test", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun malformed() {
        val uri = "#something:/test/relative/image.jpg".toUri()
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertNull(uri.path)
        assertNull(uri.filePath)
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
        assertNull(uri.filePath)
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
        assertEquals("/photo-1550939810-cb345b2f4ad7", uri.filePath)
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
        assertEquals("/上海+中國", uri.filePath)
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
        assertEquals("/something ", uri.filePath)
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
        assertEquals("/上海+中國%", uri.filePath)
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
        assertEquals("/test/image.jpg", uri.filePath)
        assertEquals(listOf("test", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun noPath() {
        val uri = "https://example.com?a=b#c".toUri()
        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.authority)
        assertNull(uri.path)
        assertNull(uri.filePath)
        assertEquals(listOf(), uri.pathSegments)
        assertEquals("a=b", uri.query)
        assertEquals("c", uri.fragment)
    }

    @Test
    fun windowsPath() {
        val uri = "D:\\test\\relative\\image.jpg".toUri(separator = "\\")
        assertNull(uri.scheme)
        assertNull(uri.authority)
        assertEquals("D:/test/relative/image.jpg", uri.path)
        assertEquals("D:\\test\\relative\\image.jpg", uri.filePath)
        assertEquals(listOf("D:", "test", "relative", "image.jpg"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun windowsPathFormattedAsUri() {
        val uri = "file:///H:/1.png".toUri(separator = "\\")
        assertEquals("file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("/H:/1.png", uri.path)
        assertEquals("H:\\1.png", uri.filePath)
        assertEquals(listOf("H:", "1.png"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun multipleSchemeSegments() {
        // This format is used for Compose multiplatform resources on Android/JVM.
        val uri = "jar:file:/outer/path/test.apk!/internal/path/1.png".toUri()
        assertEquals("jar:file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("/outer/path/test.apk!/internal/path/1.png", uri.path)
        assertEquals("/outer/path/test.apk!/internal/path/1.png", uri.filePath)
        assertEquals(listOf("outer", "path", "test.apk!", "internal", "path", "1.png"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun ipv6() {
        val uri = "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]/".toUri()
        assertEquals("http", uri.scheme)
        assertEquals("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]", uri.authority)
        assertEquals("/", uri.path)
        assertNull(uri.filePath)
        assertEquals(listOf(), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun data() {
        val uri = "data:image/png;base64,FAKE_DATA".toUri()
        assertEquals("data", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("image/png;base64,FAKE_DATA", uri.path)
        assertEquals("image/png;base64,FAKE_DATA", uri.filePath)
        assertEquals(listOf("image", "png;base64,FAKE_DATA"), uri.pathSegments)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }
}
