@file:Suppress("NOTHING_TO_INLINE")

package coil.sample

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import coil.size.PixelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlin.random.Random

inline fun <reified V : View> ViewGroup.inflate(
    @LayoutRes layoutRes: Int,
    attachToRoot: Boolean = false
): V = LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot) as V

inline val AndroidViewModel.context: Context
    get() = getApplication()

inline fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

inline fun WindowInsets.toCompat(): WindowInsetsCompat {
    return WindowInsetsCompat.toWindowInsetsCompat(this)
}

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
        .setBackgroundThreadExecutor(Dispatchers.IO.asExecutor())
        .build()
}
