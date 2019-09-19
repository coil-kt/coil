package coil.map

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.base.test.R
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ResourceIntMapperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mapper: ResourceIntMapper

    @Before
    fun before() {
        mapper = ResourceIntMapper(context)
    }

    @Test
    fun resourceInt() {
        val resId = R.drawable.normal
        val expected = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId")
        val actual = mapper.map(resId)

        assertEquals(expected, actual)
    }

    @Test
    fun invalidResourceInt() {
        val resId = 0

        assertFalse(mapper.handles(resId))
    }
}
