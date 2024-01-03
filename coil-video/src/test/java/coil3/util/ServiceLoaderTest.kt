package coil3.util

import coil3.decode.VideoFrameDecoder
import kotlin.test.assertTrue
import org.junit.Test

class ServiceLoaderTest {

    @Test
    fun serviceLoaderFindsVideoFrameDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is VideoFrameDecoder.Factory })
    }
}
