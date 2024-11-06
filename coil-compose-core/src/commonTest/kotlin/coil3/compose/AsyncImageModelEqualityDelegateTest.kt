package coil3.compose

import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncImageModelEqualityDelegateTest : RobolectricTest() {
    private val equalityDelegate = AsyncImageModelEqualityDelegate.Default

    @Test
    fun string_Equals() {
        val model1 = "https://example.com/image.jpg"
        val model2 = "https://example.com/image.jpg"
        assertTrue { equalityDelegate.equals(model1, model2) }
    }

    @Test
    fun string_NotEquals() {
        val model1 = "https://example.com/image.jpg"
        val model2 = "https://example.com/other_image.jpg"
        assertFalse { equalityDelegate.equals(model1, model2) }
    }

    @Test
    fun ImageRequest_Equals() {
        val request1 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .crossfade(true)
            .build()
        val request2 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .crossfade(false)
            .build()
        assertTrue { equalityDelegate.equals(request1, request2) }
    }

    @Test
    fun ImageRequest_NotEquals() {
        val request1 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .memoryCacheKey("one")
            .crossfade(true)
            .build()
        val request2 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .memoryCacheKey("two")
            .crossfade(false)
            .build()
        assertFalse { equalityDelegate.equals(request1, request2) }
    }
}
