package sample.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

inline fun <reified V : View> ViewGroup.inflate(
    @LayoutRes layoutRes: Int,
    attachToRoot: Boolean = false,
): V = LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot) as V

fun <T> DiffUtil.ItemCallback<T>.asConfig(): AsyncDifferConfig<T> {
    return AsyncDifferConfig.Builder(this)
        .setBackgroundThreadExecutor(Dispatchers.IO.asExecutor())
        .build()
}
