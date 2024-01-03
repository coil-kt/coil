package coil3.util

import coil3.decode.GifDecoder
import kotlin.test.assertTrue
import org.junit.Test

class ServiceLoaderTest {

    @Test
    fun serviceLoaderFindsGifDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is GifDecoder.Factory })
    }
}
