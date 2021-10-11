package coil.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.util.createMockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CacheResponseTest {

    private lateinit var context: Context
    private lateinit var server: MockWebServer

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = createMockWebServer(IMAGE)
    }

    @Test
    fun `can serialize and deserialize cache response`() {
        val url = server.url(IMAGE)
        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()
        val expected = CacheResponse(response)

        val buffer = Buffer()
        expected.writeTo(buffer)
        val actual = CacheResponse(buffer)

        assertEquals(expected.sentRequestAtMillis, actual.sentRequestAtMillis)
        assertEquals(expected.receivedResponseAtMillis, actual.receivedResponseAtMillis)
        assertEquals(expected.isTls, actual.isTls)
        assertEquals(expected.responseHeaders, actual.responseHeaders)
    }

    companion object {
        private const val IMAGE = "normal.jpg"
    }
}
