@file:Suppress("EXPERIMENTAL_API_USAGE")

package coil.fetch

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.size.PixelSize
import coil.util.createOptions
import coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContentResolver
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class UriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var loader: UriFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun before() {
        mainDispatcher = createTestMainDispatcher()
        loader = UriFetcher(context)
        pool = FakeBitmapPool()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `basic asset load`() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertTrue(loader.handles(uri))
        assertEquals(uri.toString(), loader.key(uri))

        val contentResolver: ShadowContentResolver = Shadow.extract(context.contentResolver)
        contentResolver.registerInputStream(uri, context.assets.open("normal.jpg"))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }
}
