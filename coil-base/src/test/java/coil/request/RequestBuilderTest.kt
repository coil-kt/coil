package coil.request

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class RequestBuilderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Regression test: https://github.com/coil-kt/coil/issues/221 */
    @Test
    fun `crossfade false creates no transition`() {
        val loader = ImageLoader(context) {
            crossfade(false)
        }

        val request = LoadRequest(context, loader.defaults) {
            crossfade(false)
        }

        assertNull(request.transition)

        loader.shutdown()
    }
}
