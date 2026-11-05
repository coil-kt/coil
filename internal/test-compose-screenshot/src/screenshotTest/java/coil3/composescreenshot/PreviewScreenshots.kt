package coil3.composescreenshot

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import coil3.ColorImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.rememberAsyncImagePainter
import coil3.test.composescreenshot.R

@OptIn(ExperimentalCoilApi::class)
class PreviewScreenshots {
    private val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Red.toArgb(), width = 100, height = 100)
    }

    @Preview(
        device = Devices.PIXEL,
        showBackground = true,
    )
    @Composable
    fun asyncImage() {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader(context) }

        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            AsyncImage(
                model = Unit,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.None,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Preview(
        device = Devices.PIXEL,
        showBackground = true,
    )
    @Composable
    fun rememberAsyncImagePainter() {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader(context) }

        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = Unit,
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/2489 */
    @Preview(
        device = Devices.PIXEL,
        showBackground = true,
    )
    @Composable
    fun vector() {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader(context) }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            AsyncImage(
                model = R.drawable.ic_tinted_vector,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/3001 */
    @Preview(name = "1 - Light", device = Devices.PIXEL, showBackground = true)
    @Preview(
        name = "2 - Dark",
        device = Devices.PIXEL,
        showBackground = true,
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(name = "3 - Large", device = Devices.PIXEL, showBackground = true, fontScale = 2f)
    @Composable
    fun stateGatedPreview() {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader(context) }

        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            val painter = rememberAsyncImagePainter(
                model = "https://example.com/image.jpg",
                imageLoader = imageLoader,
            )
            val state by painter.state.collectAsState()
            if (state is AsyncImagePainter.State.Success) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.None,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}
