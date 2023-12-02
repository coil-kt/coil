package coil3.map

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.base.test.R
import coil3.request.Options
import coil3.toUri
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test

class ResourceIntMapperTest {

    private lateinit var context: Context
    private lateinit var mapper: ResourceIntMapper

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mapper = ResourceIntMapper()
    }

    @Test
    fun resourceInt() {
        val resId = R.drawable.normal
        val expected = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/$resId".toUri()
        val actual = mapper.map(resId, Options(context))

        assertEquals(expected, actual)
    }

    @Test
    fun invalidResourceInt() {
        val resId = 0

        assertNull(mapper.map(resId, Options(context)))
    }
}
