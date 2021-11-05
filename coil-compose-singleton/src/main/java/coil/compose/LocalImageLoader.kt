@file:Suppress("unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.imageLoader

/**
 * A pseudo-[CompositionLocal] that returns the current [ImageLoader] for the composition.
 * If a local [ImageLoader] has not been provided, it returns the singleton [ImageLoader].
 */
val LocalImageLoader = ImageLoaderProvidableCompositionLocal()

/** @see LocalImageLoader */
@JvmInline
value class ImageLoaderProvidableCompositionLocal internal constructor(
    private val delegate: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }
) {

    val current: ImageLoader
        @Composable get() = delegate.current ?: LocalContext.current.imageLoader

    infix fun provides(value: ImageLoader) = delegate provides value

    infix fun providesDefault(value: ImageLoader) = delegate providesDefault value
}
