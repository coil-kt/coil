package coil.compose

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.IdRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import coil.util.isSimilarTo
import kotlin.test.assertTrue

fun resourceUri(id: Int): Uri {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
}

fun ImageBitmap.assertIsSimilarTo(@IdRes resId: Int) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val expected = context.getDrawable(resId)!!.toBitmap(width, height)
    assertTrue(asAndroidBitmap().isSimilarTo(expected, threshold = 0.95))
}
