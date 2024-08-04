package coil3.test.composeuimultiplatform

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import coil3.Image
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.State
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.test.utils.context
import io.coil_kt.coil3.test_compose_ui_multiplatform.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class, ExperimentalTestApi::class)
class ComposeUiResourcesTest {

    @Test
    fun drawableResource() = testComposeUiResource(
        uri = Res.getUri("drawable/sample.jpg"),
    ) {
        assertEquals(1024, width)
        assertEquals(1326, height)
    }

    @Test
    fun fileResource() = testComposeUiResource(
        uri = Res.getUri("files/sample.jpg"),
    ) {
        assertEquals(1024, width)
        assertEquals(1326, height)
    }

    private fun testComposeUiResource(
        uri: String,
        assert: Image.() -> Unit,
    ) = runComposeUiTest {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(Size.ORIGINAL)
            .build()
        var state: State = State.Empty
        setContent {
            AsyncImage(
                model = request,
                contentDescription = null,
                imageLoader = imageLoader,
                onState = { state = it },
            )
        }
        waitUntil {
            when (val localState = state) {
                is State.Error -> throw localState.result.throwable
                is State.Success -> localState.result.image.assert().let { true }
                else -> false
            }
        }
    }
}
