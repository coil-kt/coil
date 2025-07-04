package coil3.map

import coil3.ComponentRegistry
import coil3.PlatformContext
import coil3.Uri
import coil3.request.Options
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import platform.Foundation.NSURL

class NSURLMapperTest {

    @Test
    fun componentRegistry() {
        val url = NSURL(string = "https://www.example.com/image.jpg")
        val components = ComponentRegistry.Builder()
            .add(NSURLMapper())
            .build()
        val uri = components.map(url, Options(PlatformContext.INSTANCE))

        assertIs<Uri>(uri)
        assertEquals("https", uri.scheme)
        assertEquals("www.example.com", uri.authority)
        assertEquals("/image.jpg", uri.path)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun https() {
        val url = NSURL(string = "https://www.example.com/image.jpg?auth=12345")
        val uri = NSURLMapper().map(url, Options(PlatformContext.INSTANCE))

        assertIs<Uri>(uri)
        assertEquals("https", uri.scheme)
        assertEquals("www.example.com", uri.authority)
        assertEquals("/image.jpg", uri.path)
        assertEquals("auth=12345", uri.query)
        assertNull(uri.fragment)
    }

    @Test
    fun file() {
        val url = NSURL(string = "file:///path/to/a/file.jpg")
        val uri = NSURLMapper().map(url, Options(PlatformContext.INSTANCE))

        assertIs<Uri>(uri)
        assertEquals("file", uri.scheme)
        assertEquals("", uri.authority)
        assertEquals("/path/to/a/file.jpg", uri.path)
        assertNull(uri.query)
        assertNull(uri.fragment)
    }
}
