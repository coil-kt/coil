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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ViewSizeResolverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `view is already measured`() {
        val view = View(context)
        val resolver = ViewSizeResolver(view)

        view.layoutParams = ViewGroup.LayoutParams(100, 100)
        view.setPadding(10)

        val size = runBlocking {
            resolver.size()
        }

        assertEquals(PixelSize(80, 80), size)
    }

    @Test
    fun `suspend until view is measured`() {
        val view = View(context)
        val resolver = ViewSizeResolver(view)

        val deferred = GlobalScope.async(Dispatchers.Main.immediate) {
            resolver.size()
        }

        view.setPadding(20)
        view.measure(160, 160)
        view.layout(0, 0, 160, 160)
        view.viewTreeObserver.dispatchOnPreDraw()

        val size = runBlocking {
            deferred.await()
        }
        assertEquals(PixelSize(120, 120), size)
    }

    @Test
    fun `wrap_content is treated as 1px`() {
        val view = View(context)
        val resolver = ViewSizeResolver(view)

        val deferred = GlobalScope.async(Dispatchers.Main.immediate) {
            resolver.size()
        }

        view.measure(ViewGroup.LayoutParams.WRAP_CONTENT, 100)
        view.layout(0, 0, 0, 100)
        view.viewTreeObserver.dispatchOnPreDraw()

        val size = runBlocking {
            deferred.await()
        }
        assertEquals(PixelSize(1, 100), size)
    }
}
