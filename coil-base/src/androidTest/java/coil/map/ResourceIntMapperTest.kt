package coil.map

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.base.test.R
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ResourceIntMapperTest {

    private lateinit var context: Context
    private lateinit var mapper: ResourceIntMapper

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mapper = ResourceIntMapper(context)
    }

    @Test
    fun resourceInt() {
        val resId = R.drawable.normal
        val expected = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/$resId".toUri()
        val actual = mapper.map(resId)

        assertEquals(expected, actual)
    }

    @Test
    fun invalidResourceInt() {
        val resId = 0

        assertFalse(mapper.handles(resId))
    }
}
