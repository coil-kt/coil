package coil3.fetch

import coil3.ImageLoader
import coil3.request.Options
import coil3.test.utils.context
import coil3.toUri
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BlobUriFetcherTest {
    private val factory = BlobUriFetcher.Factory()

    @Test
    fun handlesBlobUriWithHttpOrigin() {
        val uri = "blob:http://localhost:8081/a3953f30-e6ef-47b6-8750-731ce8afe0f8".toUri()
        assertNotNull(factory.create(uri, Options(context), ImageLoader(context)))
    }

    @Test
    fun handlesBlobUriWithOpaqueOrigin() {
        val uri = "blob:null/a3953f30-e6ef-47b6-8750-731ce8afe0f8".toUri()
        assertNotNull(factory.create(uri, Options(context), ImageLoader(context)))
    }

    @Test
    fun ignoresNonBlobUri() {
        val uri = "https://www.example.com/image.jpg".toUri()
        assertNull(factory.create(uri, Options(context), ImageLoader(context)))
    }
}
