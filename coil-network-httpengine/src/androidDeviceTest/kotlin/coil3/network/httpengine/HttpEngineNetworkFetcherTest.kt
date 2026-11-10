package coil3.network.httpengine

import androidx.test.filters.SdkSuppress
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.SourceFetchResult
import coil3.network.httpengine.internal.HttpEngineNetworkFetcherServiceLoaderTarget
import coil3.request.Options
import coil3.test.utils.context
import coil3.toUri
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

@SdkSuppress(minSdkVersion = 34)
class HttpEngineNetworkFetcherTest {

    @Test
    fun serviceLoaderFactoryFetchesWithDefaultEngineAndExecutor() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("image-data").setHeader("Content-Type", "image/png"))
            server.start()
            val imageLoader = ImageLoader.Builder(context).diskCache(null).build()
            try {
                val target = ServiceLoaderComponentRegistry.fetchers
                    .filterIsInstance<HttpEngineNetworkFetcherServiceLoaderTarget>()
                    .single()
                val factory = assertNotNull(target.factory())
                val options = Options(context)
                assertNull(factory.create("file:///image.png".toUri(), options, imageLoader))
                val fetcher = assertNotNull(
                    factory.create(server.url("/image.png").toString().toUri(), options, imageLoader),
                )

                val result = withContext(Dispatchers.IO) {
                    assertIs<SourceFetchResult>(fetcher.fetch())
                }
                result.source.use { assertEquals("image-data", it.source().readUtf8()) }
                assertEquals(DataSource.NETWORK, result.dataSource)
                assertEquals("image/png", result.mimeType)
            } finally {
                imageLoader.shutdown()
            }
        }
    }
}
