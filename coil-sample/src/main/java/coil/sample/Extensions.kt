@file:Suppress("NOTHING_TO_INLINE")

package coil.sample

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import coil.size.PixelSize
import kotlin.random.Random

inline fun <reified V : View> ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): V {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot) as V
}

inline fun <T> unsafeLazy(noinline initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun <reified R : Any> Array<*>.findInstance(): R? = find { it is R } as R?

inline val AndroidViewModel.context: Context
    get() = getApplication()

inline fun <T> LiveData<T>.requireValue(): T = value!!

fun <V : View> Activity.bindView(@IdRes id: Int) = unsafeLazy { findViewById<V>(id) }

fun Context.getDisplaySize(): PixelSize {
    return resources.displayMetrics.run { PixelSize(widthPixels, heightPixels) }
}

fun Int.dp(context: Context): Float {
    return this * context.resources.displayMetrics.density
}

@ColorInt
fun randomColor(): Int {
    return Color.argb(128, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
}
