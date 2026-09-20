package coil3.size

import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ViewSizeResolverTest : RobolectricTest() {

    private lateinit var view: View
    private lateinit var resolver: ViewSizeResolver<View>
    private lateinit var scope: CoroutineScope

    @Before
    fun before() {
        view = View(context)
        resolver = ViewSizeResolver(view)
        scope = CoroutineScope(Dispatchers.Main.immediate)
    }

    @After
    fun after() {
        scope.cancel()
    }

    @Test
    fun `view is already measured`() = runTest {
        view.layoutParams = ViewGroup.LayoutParams(100, 100)
        view.setPadding(10)

        val size = resolver.size()

        assertEquals(Size(80, 80), size)
    }

    @Test
    fun `view padding is ignored`() = runTest {
        resolver = ViewSizeResolver(view, subtractPadding = false)
        view.layoutParams = ViewGroup.LayoutParams(100, 100)
        view.setPadding(10)

        val size = resolver.size()

        assertEquals(Size(100, 100), size)
    }

    @Test
    fun `suspend until view is measured`() = runTest {
        val deferred = scope.async(Dispatchers.Main.immediate) {
            resolver.size()
        }

        // Predraw passes should be ignored until the view is measured.
        view.viewTreeObserver.dispatchOnPreDraw()
        assertTrue(deferred.isActive)
        view.viewTreeObserver.dispatchOnPreDraw()
        assertTrue(deferred.isActive)

        view.setPadding(20)
        view.layoutParams = ViewGroup.LayoutParams(160, 160)
        view.measure(160, 160)
        view.layout(0, 0, 160, 160)
        view.viewTreeObserver.dispatchOnPreDraw()

        val size = deferred.await()
        assertEquals(Size(120, 120), size)
    }

    @Test
    fun `wrap_content is resolved to the size of the image`() = runTest {
        val deferred = scope.async(Dispatchers.Main.immediate) {
            resolver.size()
        }

        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 100)
        view.measure(ViewGroup.LayoutParams.WRAP_CONTENT, 100)
        view.layout(0, 0, 0, 100)
        view.viewTreeObserver.dispatchOnPreDraw()

        val size = deferred.await()
        assertEquals(Size(Dimension.Undefined, 100), size)
    }
}
