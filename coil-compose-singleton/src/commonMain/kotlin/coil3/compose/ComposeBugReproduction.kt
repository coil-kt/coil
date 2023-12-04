package coil3.compose

import androidx.compose.runtime.Composable
import coil3.ImageLoader

@Composable
fun ComposeBugReproduction() {
    SubcomposeAsyncImage(
        model = Unit,
        contentDescription = null,
        imageLoader = ImageLoader(LocalPlatformContext.current),
    )
}
