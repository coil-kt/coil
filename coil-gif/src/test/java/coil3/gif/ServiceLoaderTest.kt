package coil3.gif

import coil3.test.utils.RobolectricTest
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.assertTrue
import org.junit.Test

class ServiceLoaderTest : RobolectricTest() {

    @Test
    fun serviceLoaderFindsGifDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is GifDecoder.Factory })
    }
}
