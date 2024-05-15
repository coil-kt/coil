package coil3.composescreenshot

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.placeholder

class PreviewScreenshots {

    @Preview(
        device = Devices.PIXEL,
        showBackground = true,
    )
    @Composable
    fun asyncImage() {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader(context) }

        AsyncImage(
            // AsyncImagePainter's default preview behaviour displays the request's placeholder.
            model = ImageRequest.Builder(context)
                .data(Unit)
                .placeholder(
                    object : ColorDrawable(Color.RED) {
                        override fun getIntrinsicWidth() = 100
                        override fun getIntrinsicHeight() = 100
                    },
                )
                .build(),
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.None,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Preview(
        device = Devices.PIXEL,
        showBackground = true,
    )
    @Composable
    fun rememberAsyncImagePainter() {
        val context = LocalContext.current
        val imageLoader = remember { ImageLoader(context) }

        Image(
            painter = rememberAsyncImagePainter(
                // AsyncImagePainter's default preview behaviour displays the request's placeholder.
                model = ImageRequest.Builder(context)
                    .data(Unit)
                    .placeholder(
                        object : ColorDrawable(Color.RED) {
                            override fun getIntrinsicWidth() = 100
                            override fun getIntrinsicHeight() = 100
                        },
                    )
                    .build(),
                imageLoader = imageLoader,
            ),
            contentDescription = null,
            contentScale = ContentScale.None,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
