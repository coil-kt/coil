package coil3.svg

import coil3.decode.DecodeUtils
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.Test
import kotlin.test.assertTrue
import okio.Buffer

class SvgDecoderTest {

    /** Regression test: https://github.com/coil-kt/coil/issues/1154 */
    @Test
    fun isSvg_newLine() {
        val text = """
            <svg
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:cc="http://creativecommons.org/ns#"
            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:svg="http://www.w3.org/2000/svg"
            xmlns="http://www.w3.org/2000/svg"
            xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"/>
            """.trimIndent()
        val buffer = Buffer().apply { writeUtf8(text) }

        assertTrue(DecodeUtils.isSvg(buffer))
    }

    @Test
    fun serviceLoaderFindsSvgDecoder() {
        val decoders = ServiceLoaderComponentRegistry.decoders
        assertTrue(decoders.any { it.factory() is SvgDecoder.Factory })
    }
}
