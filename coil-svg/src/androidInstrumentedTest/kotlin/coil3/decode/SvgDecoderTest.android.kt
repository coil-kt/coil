package coil3.decode

import coil3.test.utils.AbstractSvgDecoderTest
import kotlin.test.Test

class SvgDecoderTestAndroid : AbstractSvgDecoderTest(SvgDecoder.Factory()) {
    @Test
    override fun handlesSvgMimeType() =
        super.handlesSvgMimeType()

    @Test
    override fun doesNotHandlePngMimeType() =
        super.handlesSvgMimeType()

    @Test
    override fun doesNotHandleGeneralXmlFile() =
        super.doesNotHandleGeneralXmlFile()

    @Test
    override fun handlesSvgSource() =
        super.handlesSvgSource()

    @Test
    override fun basic() =
        super.basic()

    @Test
    override fun noViewBox() =
        super.noViewBox()

    /** Regression test: https://github.com/coil-kt/coil/issues/1246 */
    @Test
    override fun oneDimensionIsUndefined() =
        super.oneDimensionIsUndefined()
}
