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
import kotlin.test.assertTrue

class ResourceUriMapperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mapper: ResourceUriMapper

    @Before
    fun before() {
        mapper = ResourceUriMapper(context)
    }

    @Test
    fun resourceNameUri() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/drawable/normal")
        val expected = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.drawable.normal}")

        assertTrue(mapper.handles(uri))

        val actual = mapper.map(uri)

        assertEquals(expected, actual)
    }

    @Test
    fun resourceIntUri() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.drawable.normal}")

        assertFalse(mapper.handles(uri))
    }
}
