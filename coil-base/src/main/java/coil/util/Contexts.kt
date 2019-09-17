@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

internal val HAS_APPCOMPAT_RESOURCES = try {
    Class.forName(AppCompatResources::class.java.name)
    true
} catch (ignored: Throwable) {
    false
}

internal fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable {
    val drawable = if (HAS_APPCOMPAT_RESOURCES) {
        AppCompatResources.getDrawable(this, resId)
    } else {
        ContextCompat.getDrawable(this, resId)
    }
    return checkNotNull(drawable)
}

internal fun Context.getLifecycle(): Lifecycle? {
    var context = this
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
