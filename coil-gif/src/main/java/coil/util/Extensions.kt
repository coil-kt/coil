@file:JvmName("-GifExtensions")

package coil.util

import android.graphics.PixelFormat
import android.graphics.PostProcessor
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.transform.AnimatedTransformation
import coil.transform.PixelOpacity

@RequiresApi(Build.VERSION_CODES.P)
internal fun AnimatedTransformation.asPostProcessor() = PostProcessor { canvas -> transform(canvas).flag }

internal val PixelOpacity.flag: Int
    get() = when (this) {
        PixelOpacity.UNCHANGED -> PixelFormat.UNKNOWN
        PixelOpacity.TRANSLUCENT -> PixelFormat.TRANSLUCENT
        PixelOpacity.OPAQUE -> PixelFormat.OPAQUE
    }

@RequiresApi(Build.VERSION_CODES.M)
internal fun animatable2CallbackOf(
    onStart: (() -> Unit)?,
    onEnd: (() -> Unit)?
) = object : Animatable2.AnimationCallback() {
    override fun onAnimationStart(drawable: Drawable?) {
        onStart?.invoke()
    }
    override fun onAnimationEnd(drawable: Drawable?) {
        onEnd?.invoke()
    }
}

internal fun animatable2CompatCallbackOf(
    onStart: (() -> Unit)?,
    onEnd: (() -> Unit)?
) = object : Animatable2Compat.AnimationCallback() {
    override fun onAnimationStart(drawable: Drawable?) {
        onStart?.invoke()
    }
    override fun onAnimationEnd(drawable: Drawable?) {
        onEnd?.invoke()
    }
}
