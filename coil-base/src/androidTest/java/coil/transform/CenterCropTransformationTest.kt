package coil.transform

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CenterCropTransformationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var transformation: CenterCropTransformation

    @Before
    fun before() {
        pool = FakeBitmapPool()
    }

    @Test
    fun withBitmap_assertThatTransformReturnsCorrectSize() {
        val normalBitmap = BitmapFactory.decodeStream(context.assets.open("normal.jpg"))
        val newWidth = 1000
        val newHeight = 1250
        val transformation = CenterCropTransformation(newWidth, newHeight)

        val centerCropBitmap = runBlocking {
            transformation.transform(pool, normalBitmap)
        }

        assertEquals(newWidth, centerCropBitmap.width)
        assertEquals(newHeight, centerCropBitmap.height)
    }
}
