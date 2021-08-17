@file:JvmName("-GifExtensions")

package coil.util

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.PostProcessor
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.transform.AnimatedTransformation
import coil.transform.PixelOpacity

@RequiresApi(28)
internal fun AnimatedTransformation.asPostProcessor() =
    PostProcessor { canvas -> transform(canvas).flag }

internal val PixelOpacity.flag: Int
    get() = when (this) {
        PixelOpacity.UNCHANGED -> PixelFormat.UNKNOWN
        PixelOpacity.TRANSLUCENT -> PixelFormat.TRANSLUCENT
        PixelOpacity.OPAQUE -> PixelFormat.OPAQUE
    }

@RequiresApi(23)
internal fun animatable2CallbackOf(
    onStart: (() -> Unit)?,
    onEnd: (() -> Unit)?
) = object : Animatable2.AnimationCallback() {
    override fun onAnimationStart(drawable: Drawable?) { onStart?.invoke() }
    override fun onAnimationEnd(drawable: Drawable?) { onEnd?.invoke() }
}

internal fun animatable2CompatCallbackOf(
    onStart: (() -> Unit)?,
    onEnd: (() -> Unit)?
) = object : Animatable2Compat.AnimationCallback() {
    override fun onAnimationStart(drawable: Drawable?) { onStart?.invoke() }
    override fun onAnimationEnd(drawable: Drawable?) { onEnd?.invoke() }
}

internal inline fun <T> List<T>.forEachIndices(action: (T) -> Unit) {
    for (i in indices) {
        action(get(i))
    }
}

internal val Bitmap.Config.isHardware: Boolean
    get() = SDK_INT >= 26 && this == Bitmap.Config.HARDWARE
