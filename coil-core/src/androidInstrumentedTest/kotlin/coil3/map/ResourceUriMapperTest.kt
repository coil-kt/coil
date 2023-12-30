package coil3.map

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import coil3.core.test.R
import coil3.pathSegments
import coil3.request.Options
import coil3.test.utils.context
import coil3.toUri
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ResourceUriMapperTest {

    private val mapper = ResourceUriMapper()

    @Test
    fun resourceNameUri() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/normal".toUri()
        val expected = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()
        val actual = mapper.map(uri, Options(context))

        assertEquals(expected, actual)
    }

    @Test
    fun externalResourceNameUri() {
        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable-xhdpi/ic_power_system.png
        val packageName = "com.android.settings"
        val input = "$SCHEME_ANDROID_RESOURCE://$packageName/drawable/ic_power_system".toUri()
        val output = mapper.map(input, Options(context))

        assertNotNull(output)
        assertEquals(SCHEME_ANDROID_RESOURCE, output.scheme)
        assertEquals(packageName, output.authority)
        assertTrue(output.pathSegments[0].toInt() > 0)
    }

    @Test
    fun resourceIntUri() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()

        assertNull(mapper.map(uri, Options(context)))
    }
}
