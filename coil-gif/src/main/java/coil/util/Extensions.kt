@file:JvmName("-GifExtensions")

package coil.util

import android.graphics.PixelFormat
import android.graphics.PostProcessor
import androidx.annotation.RequiresApi
import coil.transform.AnimatedTransformation
import coil.transform.PixelOpacity

@RequiresApi(28)
internal fun AnimatedTransformation.asPostProcessor() = PostProcessor { canvas -> transform(canvas).flag }

internal val PixelOpacity.flag: Int
    get() = when (this) {
        PixelOpacity.UNCHANGED -> PixelFormat.UNKNOWN
        PixelOpacity.TRANSLUCENT -> PixelFormat.TRANSLUCENT
        PixelOpacity.OPAQUE -> PixelFormat.OPAQUE
    }
