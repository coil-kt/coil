package coil3.network.httpengine.internal

import androidx.test.filters.SdkSuppress
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.Test
import kotlin.test.assertTrue

@SdkSuppress(minSdkVersion = 34)
class HttpEngineNetworkFetcherServiceLoaderTest {

    @Test
    fun serviceLoaderFindsFetcher() {
        val fetchers = ServiceLoaderComponentRegistry.fetchers
        assertTrue(
            fetchers.any {
                it is HttpEngineNetworkFetcherServiceLoaderTarget && it.factory() != null
            },
        )
    }
}
