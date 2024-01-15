package coil3.video

import coil3.test.utils.RobolectricTest
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.assertTrue
import org.junit.Test

class VideoFrameServiceLoaderTest : RobolectricTest() {

    @Test
    fun serviceLoaderFindsVideoFrameDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is VideoFrameDecoder.Factory })
    }

    @Test
    fun serviceLoaderFindsMediaDataSourceFetcher() {
        val fetchers = ServiceLoaderComponentRegistry.fetchers
        assertTrue(fetchers.any { it.factory() is MediaDataSourceFetcher.Factory })
    }
}
