package coil3.network

import android.graphics.ImageDecoder
import android.os.Build
import coil3.Extras
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import okio.Buffer
import okio.ByteString.Companion.toByteString
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Config(shadows = [AcceptHeadersTest.CustomShadowImageDecoder::class])
class AcceptHeadersTest : RobolectricTest() {

    @AfterTest
    fun tearDown() {
        CustomShadowImageDecoder.clearSupportedMimeTypes()
    }

    @Test
    @Config(sdk = [Config.OLDEST_SDK])
    fun allAndroidsSupportBasicFormats() = testAcceptHeader(
        isAvifSupported = false,
        expectedValue = "image/bmp;q=0.1,image/gif;q=0.1,image/jpeg;q=0.2,image/pjpg;q=0.2,image/png;q=0.3,image/webp;q=0.4",
    )

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun android8SupportsHeic() = testAcceptHeader(
        isAvifSupported = false,
        expectedValue = "image/bmp;q=0.1,image/gif;q=0.1,image/jpeg;q=0.2,image/pjpg;q=0.2,image/png;q=0.3,image/webp;q=0.4,image/heic;q=0.5",
    )

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun android12DoesntHaveToSupportsAvif() = testAcceptHeader(
        isAvifSupported = false,
        expectedValue = "image/bmp;q=0.1,image/gif;q=0.1,image/jpeg;q=0.2,image/pjpg;q=0.2,image/png;q=0.3,image/webp;q=0.4,image/heic;q=0.5",
    )

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun android14SupportsAvif() = testAcceptHeader(
        isAvifSupported = true,
        expectedValue = "image/bmp;q=0.1,image/gif;q=0.1,image/jpeg;q=0.2,image/pjpg;q=0.2,image/png;q=0.3,image/webp;q=0.4,image/heic;q=0.5,image/avif;q=0.6",
    )

    @Test
    @Config(sdk = [Config.NEWEST_SDK])
    fun acceptHeaderIsPassedThrough() = testAcceptHeader(
        isAvifSupported = true,
        headers = NetworkHeaders.Builder().set("accept", "image/gif").build(),
        expectedValue = "image/gif",
    )

    private fun testAcceptHeader(
        isAvifSupported: Boolean,
        headers: NetworkHeaders = NetworkHeaders.EMPTY,
        expectedValue: String,
    ) = runTestAsync {
        if (isAvifSupported) CustomShadowImageDecoder.addSupportedMimeType("image/avif")

        val expectedSize = 1_000
        val url = "https://example.com/image.jpg"
        val method = "POST"
        val body = NetworkRequestBody(ByteArray(500).toByteString())
        val options = Options(
            context = context,
            extras = Extras.Builder()
                .set(Extras.Key.httpMethod, method)
                .set(Extras.Key.httpHeaders, headers)
                .set(Extras.Key.httpBody, body)
                .build(),
        )
        val networkClient = FakeNetworkClient(
            respond = {
                NetworkResponse(
                    body = NetworkResponseBody(
                        source = Buffer().apply { write(ByteArray(expectedSize)) },
                    ),
                )
            },
        )
        val result = NetworkFetcher(
            url = url,
            options = options,
            networkClient = lazyOf(networkClient),
            diskCache = lazyOf(null),
            cacheStrategy = lazyOf(CacheStrategy.DEFAULT),
            connectivityChecker = lazyOf(ConnectivityChecker(context)),
            concurrentRequestStrategy = lazyOf(ConcurrentRequestStrategy.UNCOORDINATED),
        ).fetch()

        assertIs<SourceFetchResult>(result)

        assertEquals(networkClient.requests.single().headers["accept"], expectedValue)
    }

    class FakeNetworkClient(
        private val respond: suspend (NetworkRequest) -> NetworkResponse,
    ) : NetworkClient {
        val requests = mutableListOf<NetworkRequest>()
        val responses = mutableListOf<NetworkResponse>()

        override suspend fun <T> executeRequest(
            request: NetworkRequest, block: suspend (response: NetworkResponse) -> T,
        ): T {
            requests += request
            val response = respond(request)
            responses += response
            return block(response)
        }
    }

    @Implements(ImageDecoder::class)
    class CustomShadowImageDecoder {
        companion object {
            private val supportedMimeTypes = mutableSetOf<String>()

            fun setSupportedMimeTypes(vararg mimeTypes: String) {
                supportedMimeTypes.clear()
                supportedMimeTypes.addAll(mimeTypes)
            }

            fun addSupportedMimeType(mimeType: String) {
                supportedMimeTypes.add(mimeType)
            }

            fun clearSupportedMimeTypes() {
                supportedMimeTypes.clear()
            }

            @Implementation
            @JvmStatic
            fun isMimeTypeSupported(mimeType: String): Boolean {
                return supportedMimeTypes.contains(mimeType)
            }
        }
    }
}
