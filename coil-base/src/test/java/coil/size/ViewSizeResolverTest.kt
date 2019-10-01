package coil.size

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.max
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ViewSizeResolverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var view: View
    private lateinit var resolver: ViewSizeResolver<View>

    @Before
    fun before() {
        view = View(context)
        resolver = ViewSizeResolver(view)
    }

    @Test
    fun `view is already measured`() {
        view.layoutParams = ViewGroup.LayoutParams(100, 100)
        view.setPadding(10)

        val size = runBlocking {
            resolver.size()
        }

        assertEquals(PixelSize(80, 80), size)
    }

    @Test
    fun `view padding is ignored`() {
        resolver = ViewSizeResolver(view, includePadding = false)
        view.layoutParams = ViewGroup.LayoutParams(100, 100)
        view.setPadding(10)

        val size = runBlocking {
            resolver.size()
        }

        assertEquals(PixelSize(100, 100), size)
    }

    @Test
    fun `suspend until view is measured`() {
        val deferred = GlobalScope.async(Dispatchers.Main.immediate) {
            resolver.size()
        }

        view.setPadding(20)
        view.layoutParams = ViewGroup.LayoutParams(160, 160)
        view.measure(160, 160)
        view.layout(0, 0, 160, 160)
        view.viewTreeObserver.dispatchOnPreDraw()

        val size = runBlocking {
            deferred.await()
        }
        assertEquals(PixelSize(120, 120), size)
    }

    @Test
    fun `wrap_content is resolved to the size of the display`() {
        val expectedWidth = view.context.resources.displayMetrics.run { max(widthPixels, heightPixels) }
        val deferred = GlobalScope.async(Dispatchers.Main.immediate) {
            resolver.size()
        }

        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 100)
        view.measure(ViewGroup.LayoutParams.WRAP_CONTENT, 100)
        view.layout(0, 0, 0, 100)
        view.viewTreeObserver.dispatchOnPreDraw()

        val size = runBlocking {
            deferred.await()
        }
        assertEquals(PixelSize(expectedWidth, 100), size)
    }
}
