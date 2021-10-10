package coil

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.fetch.Fetcher
import coil.request.ImageRequest
import coil.util.createTestMainDispatcher
import coil.util.runBlockingTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RealImageLoaderTest {

    private lateinit var context: Context
    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mainDispatcher = createTestMainDispatcher()
        imageLoader = ImageLoader.Builder(context)
            .diskCache(null)
            .build()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/933 */
    @Test
    fun executeIsCancelledIfScopeIsCancelled() = runBlockingTest {
        val isCancelled = MutableStateFlow(false)

        val scope = CoroutineScope(mainDispatcher)
        scope.launch {
            val request = ImageRequest.Builder(context)
                .data(Unit)
                .dispatcher(mainDispatcher)
                // Use a custom fetcher that suspends until cancellation.
                .fetcherFactory<Unit> { _, _, _ ->
                    Fetcher { awaitCancellation() }
                }
                .listener(onCancel = {
                    isCancelled.value = true
                })
                .build()
            imageLoader.execute(request)
        }

        assertTrue(scope.isActive)
        assertFalse(isCancelled.value)

        scope.cancel()

        // Suspend until the request is cancelled.
        isCancelled.first { it }
    }
}
