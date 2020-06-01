@file:JvmName("-Contexts")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.annotation.XmlRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

internal fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable {
    return checkNotNull(AppCompatResources.getDrawable(this, resId)) { "Invalid resource ID: $resId" }
}

internal fun Resources.getDrawableCompat(@DrawableRes resId: Int, theme: Resources.Theme?): Drawable {
    return checkNotNull(ResourcesCompat.getDrawable(this, resId, theme)) { "Invalid resource ID: $resId" }
}

/**
 * Supports inflating XML [Drawable]s from other package's resources.
 *
 * Prefer using [Context.getDrawableCompat] for resources that are part of the current package.
 */
@SuppressLint("ResourceType")
internal fun Context.getXmlDrawableCompat(resources: Resources, @XmlRes resId: Int): Drawable {
    // Find the XML's start tag.
    val parser = resources.getXml(resId)
    var type = parser.next()
    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        type = parser.next()
    }
    if (type != XmlPullParser.START_TAG) {
        throw XmlPullParserException("No start tag found.")
    }

    // Modified from androidx.appcompat.widget.ResourceManagerInternal.
    if (SDK_INT < 24) {
        when (parser.name) {
            "vector" -> {
                val attrs = Xml.asAttributeSet(parser)
                return VectorDrawableCompat.createFromXmlInner(resources, parser, attrs, theme)
            }
            "animated-vector" -> {
                val attrs = Xml.asAttributeSet(parser)
                return AnimatedVectorDrawableCompat.createFromXmlInner(this, resources, parser, attrs, theme)
            }
        }
    }

    // Fall back to the platform APIs.
    return resources.getDrawableCompat(resId, theme)
}

internal fun Context?.getLifecycle(): Lifecycle? {
    var context: Context? = this
    while (true) {
        when (context) {
            is LifecycleOwner -> return context.lifecycle
            !is ContextWrapper -> return null
            else -> context = context.baseContext
        }
    }
}

internal inline fun <reified T : Any> Context.requireSystemService(): T {
    return checkNotNull(getSystemService()) { "System service of type ${T::class.java} was not found." }
}

internal inline fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
