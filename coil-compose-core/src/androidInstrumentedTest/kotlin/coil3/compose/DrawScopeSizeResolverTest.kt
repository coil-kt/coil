package coil3.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size as CoilSize
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class DrawScopeSizeResolverTest {

    @Test
    fun imageIsLoadedWithDrawScopeSize() = runComposeUiTest {
        val expectedSizeDp = 100.dp
        var expectedSizePx = Size.Unspecified
        var actualSizePx = Size.Unspecified

        setContent {
            expectedSizePx = with(LocalDensity.current) {
                expectedSizeDp.toPx().let { Size(it, it) }
            }

            val context = LocalPlatformContext.current
            val imageLoader = remember(context) {
                ImageLoader.Builder(context)
                    .components {
                        add { chain ->
                            actualSizePx = chain.size.toComposeSize()
                            SuccessResult(
                                image = ColorImage(),
                                request = chain.request,
                            )
                        }
                    }
                    .coroutineContext(EmptyCoroutineContext)
                    .build()
            }
            val request = remember(context) {
                ImageRequest.Builder(context)
                    .data(Unit)
                    .size(DrawScopeSizeResolver())
                    .build()
            }
            val painter = rememberAsyncImagePainter(request, imageLoader)

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(expectedSizeDp),
            )
        }

        assertNotEquals(Size.Unspecified, actualSizePx)
        assertEquals(expectedSizePx, actualSizePx)
    }

    private fun CoilSize.toComposeSize() = Size(
        width = (width as Dimension.Pixels).px.toFloat(),
        height = (height as Dimension.Pixels).px.toFloat(),
    )
}
