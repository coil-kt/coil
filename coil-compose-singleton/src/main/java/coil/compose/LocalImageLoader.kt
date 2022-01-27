@file:Suppress("DEPRECATION", "unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.imageLoader

private const val DEPRECATION_MESSAGE = "" +
    "LocalImageLoader was intended to provide a method to overwrite the singleton ImageLoader " +
    "in local compositions. In practice, it's not clear that `LocalImageLoader.provide` " +
    "**does not** set the singleton ImageLoader. This can result in accidentally creating " +
    "multiple ImageLoader instances if you use a combination of `LocalImageLoader.current` and " +
    "`context.imageLoader`. To maximize performance, apps should create one ImageLoader or use " +
    "`ImageLoader.newBuilder` to create new ImageLoaders that share the same resources.\n" +
    "\n" +
    "Additionally, as a composition is at most scoped to an Activity, `LocalImageLoader.provide` " +
    "encourages creating multiple ImageLoaders if the user creates multiple activities that use " +
    "Compose.\n" +
    "\n" +
    "It's recommended to use ImageLoaderFactory to set the singleton ImageLoader and " +
    "`LocalContext.current.imageLoader` to access the singleton ImageLoader in Compose. If you " +
    "need to use a locally scoped ImageLoader it's recommended to use the AsyncImage and " +
    "rememberAsyncImagePainter overloads that have an ImageLoader argument and pass the local " +
    "ImageLoader as input."

@Deprecated(message = DEPRECATION_MESSAGE)
val LocalImageLoader = ImageLoaderProvidableCompositionLocal()

@Deprecated(message = DEPRECATION_MESSAGE)
@JvmInline
value class ImageLoaderProvidableCompositionLocal internal constructor(
    private val delegate: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }
) {

    @Deprecated(
        message = DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith(
            expression = "LocalContext.current.imageLoader",
            imports = ["androidx.compose.ui.platform.LocalContext", "coil.imageLoader"]
        )
    )
    val current: ImageLoader
        @Composable
        @ReadOnlyComposable
        get() = delegate.current ?: LocalContext.current.imageLoader

    @Deprecated(
        message = DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith(
            expression = "Coil.setImageLoader(value)",
            imports = ["coil.Coil"]
        )
    )
    infix fun provides(value: ImageLoader) = delegate provides value
}
