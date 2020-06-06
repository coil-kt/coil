package coil.request

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.transition.Transition
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class RequestBuilderTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/221 */
    @Test
    fun `crossfade false creates none transition`() {
        val loader = ImageLoader.Builder(context)
            .crossfade(false)
            .build()

        val request = Request.Builder(context)
            .crossfade(false)
            .build()

        assertEquals(Transition.NONE, request.transition)

        loader.shutdown()
    }
}
