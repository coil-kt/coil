package coil3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import coil3.core.test.R
import coil3.test.utils.assumeTrue
import coil3.test.utils.context
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test

class ImageAndroidTest {

    /** Regression test: https://github.com/coil-kt/coil/issues/2644 */
    @Test
    fun toBitmap_hardware() {
        assumeTrue(SDK_INT >= 26)

        // Decode a resource since we can't create a test hardware bitmap directly.
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.HARDWARE
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.normal, options)

        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        assertSame(bitmap, bitmap.asImage().toBitmap())
    }
}
