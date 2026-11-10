package coil3.network.httpengine.internal

import coil3.test.utils.RobolectricTest
import kotlin.test.Test
import kotlin.test.assertNull
import org.robolectric.annotation.Config

class HttpEngineNetworkFetcherServiceLoaderTest : RobolectricTest() {

    @Test
    @Config(sdk = [33])
    fun factoryIsUnavailableBelowApi34() {
        assertNull(HttpEngineNetworkFetcherServiceLoaderTarget().factory())
    }
}
