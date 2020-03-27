package coil.request

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class RequestBuilderTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/221 */
    @Test
    fun `crossfade false creates no transition`() {
        val loader = ImageLoader.Builder(context)
            .crossfade(false)
            .build()

        val request = LoadRequest.Builder(context)
            .crossfade(false)
            .build()

        assertNull(request.transition)

        loader.shutdown()
    }
}
