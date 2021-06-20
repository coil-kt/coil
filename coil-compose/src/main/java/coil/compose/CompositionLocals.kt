package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.imageLoader

val LocalImageLoader = ImageLoaderProvidableCompositionLocal()

@Suppress("CompositionLocalNaming", "NOTHING_TO_INLINE", "unused")
@JvmInline
value class ImageLoaderProvidableCompositionLocal internal constructor(
    @PublishedApi internal val delegate: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }
) {

    inline val current: ImageLoader
        @Composable get() = delegate.current ?: LocalContext.current.imageLoader

    inline infix fun provides(value: ImageLoader) = delegate provides value

    inline infix fun providesDefault(value: ImageLoader) = delegate providesDefault value

    inline fun clear() {
        delegate provides null
    }
}
