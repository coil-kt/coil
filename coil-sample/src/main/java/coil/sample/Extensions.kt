@file:Suppress("NOTHING_TO_INLINE")

package coil.sample

import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import coil.size.PixelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlin.random.Random

inline fun <reified V : View> ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): V {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot) as V
}

inline fun <reified R : Any> Array<*>.findInstance(): R? {
    for (element in this) if (element is R) return element
    return null
}

inline val AndroidViewModel.context: Context
    get() = getApplication()

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

fun <T> DiffUtil.ItemCallback<T>.asConfig(): AsyncDifferConfig<T> {
    return AsyncDifferConfig.Builder(this)
        .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
        .build()
}

@Suppress("DEPRECATION")
fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) {
    if (SDK_INT >= 30) {
        setDecorFitsSystemWindows(decorFitsSystemWindows)
    } else {
        decorView.apply {
            systemUiVisibility = if (decorFitsSystemWindows) {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION.inv()
            } else {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
        }
    }
}

val WindowInsets.systemWindowInsetTopCompat: Int
    @RequiresApi(21) @Suppress("DEPRECATION") get() {
        return if (SDK_INT >= 30) getInsets(WindowInsets.Type.systemBars()).top else systemWindowInsetTop
    }
