package coil3.composescreenshot

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.rememberAsyncImagePainter
import coil3.test.FakeImage
import coil3.test.composescreenshot.R

@OptIn(ExperimentalCoilApi::class)
class PreviewScreenshots {
    private val previewHandler = AsyncImagePreviewHandler {
        FakeImage(color = Color.RED)
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

        AsyncImage(
            model = R.drawable.ic_tinted_vector,
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.None,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
