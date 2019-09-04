package coil.map

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.base.test.R
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceUriMapperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mapper: ResourceUriMapper

    @Before
    fun before() {
        mapper = ResourceUriMapper(context)
    }

    @Test
    fun resourceIntUri() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.drawable.normal}")
        val resId = mapper.map(uri)

        assertEquals(R.drawable.normal, resId)
    }

    @Test
    fun resourceNameUri() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/drawable/normal")
        val resId = mapper.map(uri)

        assertEquals(R.drawable.normal, resId)
    }

    @Test
    fun invalidResourceUri() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/drawable/invalid_image")

        assertFailsWith<IllegalStateException> { mapper.map(uri) }
    }
}
