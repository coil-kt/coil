package coil.decode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.size.Scale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DecodeUtilsTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `inSampleSize with FILL is calculated correctly`() {
        assertEquals(DecodeUtils.calculateInSampleSize(100, 100, 50, 50, Scale.FILL), 2)

        assertEquals(DecodeUtils.calculateInSampleSize(100, 50, 50, 50, Scale.FILL), 1)

        assertEquals(DecodeUtils.calculateInSampleSize(99, 99, 50, 50, Scale.FILL), 1)

        assertEquals(DecodeUtils.calculateInSampleSize(200, 99, 50, 50, Scale.FILL), 1)

        assertEquals(DecodeUtils.calculateInSampleSize(200, 200, 50, 50, Scale.FILL), 4)

        assertEquals(DecodeUtils.calculateInSampleSize(1024, 1024, 80, 80, Scale.FILL), 8)

        assertEquals(DecodeUtils.calculateInSampleSize(50, 50, 100, 100, Scale.FILL), 1)
    }

    @Test
    fun `inSampleSize with FIT is calculated correctly`() {
        assertEquals(DecodeUtils.calculateInSampleSize(100, 100, 50, 50, Scale.FIT), 2)

        assertEquals(DecodeUtils.calculateInSampleSize(100, 50, 50, 50, Scale.FIT), 2)

        assertEquals(DecodeUtils.calculateInSampleSize(99, 99, 50, 50, Scale.FIT), 1)

        assertEquals(DecodeUtils.calculateInSampleSize(200, 99, 50, 50, Scale.FIT), 4)

        assertEquals(DecodeUtils.calculateInSampleSize(200, 200, 50, 50, Scale.FIT), 4)

        assertEquals(DecodeUtils.calculateInSampleSize(160, 1024, 80, 80, Scale.FIT), 8)

        assertEquals(DecodeUtils.calculateInSampleSize(50, 50, 100, 100, Scale.FIT), 1)
    }
}
