package coil3.decode

import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.Test
import kotlin.test.assertTrue
import okio.Buffer

class SvgDecoderTest {

    /** Regression test: https://github.com/coil-kt/coil/issues/1154 */
    @Test
    fun isSvg_newLine() {
        val text = "<svg\n" +
            "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
            "   xmlns:cc=\"http://creativecommons.org/ns#\"\n" +
            "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "   xmlns:svg=\"http://www.w3.org/2000/svg\"\n" +
            "   xmlns=\"http://www.w3.org/2000/svg\"\n" +
            "   xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\"/>"
        val buffer = Buffer().apply { writeUtf8(text) }

        assertTrue(DecodeUtils.isSvg(buffer))
    }

    @Test
    fun serviceLoaderFindsSvgDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is SvgDecoder.Factory })
    }
}
