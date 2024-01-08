package coil3.video

import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.assertTrue
import org.junit.Test

class ServiceLoaderTest {

    @Test
    fun serviceLoaderFindsVideoFrameDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is VideoFrameDecoder.Factory })
    }
}
