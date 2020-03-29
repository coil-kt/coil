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
import kotlin.test.assertTrue

class ResourceUriMapperTest {

    private lateinit var context: Context
    private lateinit var mapper: ResourceUriMapper

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mapper = ResourceUriMapper(context)
    }

    @Test
    fun resourceNameUri() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/normal".toUri()
        val expected = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()

        assertTrue(mapper.handles(uri))

        val actual = mapper.map(uri)

        assertEquals(expected, actual)
    }

    @Test
    fun externalResourceNameUri() {
        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable-xhdpi/ic_power_system.png
        val packageName = "com.android.settings"
        val input = "$SCHEME_ANDROID_RESOURCE://$packageName/drawable/ic_power_system".toUri()

        assertTrue(mapper.handles(input))

        val output = mapper.map(input)

        assertEquals(SCHEME_ANDROID_RESOURCE, output.scheme)
        assertEquals(packageName, output.authority)
        assertTrue(output.pathSegments[0].toInt() > 0)
    }

    @Test
    fun resourceIntUri() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()

        assertFalse(mapper.handles(uri))
    }
}
