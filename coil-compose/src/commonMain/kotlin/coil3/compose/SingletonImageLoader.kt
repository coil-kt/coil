package coil3.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi

/**
 * Alias for [SingletonImageLoader.setSafe] that's optimized for calling from Compose.
 */
@ExperimentalCoilApi
@Composable
@ReadOnlyComposable
fun setSingletonImageLoaderFactory(factory: (context: PlatformContext) -> ImageLoader) {
    // This can't be invoked inside a LaunchedEffect as it needs to run immediately before
    // SingletonImageLoader.get is called by any composables.
    SingletonImageLoader.setSafe(factory)
}
