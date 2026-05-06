package coil3.network.okhttp

import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Scale
import coil3.test.utils.context
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class Issue3406Test {

    @Test
    fun unsplashProfileImage64LoadsWithImageLoaderExecute() = runTest {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(UNSPLASH_PROFILE_IMAGE_64)
            .size(192, 192)
            .scale(Scale.FILL)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable

        assertIs<SuccessResult>(result)
    }

    companion object {
        private const val UNSPLASH_PROFILE_IMAGE_64 =
            "https://images.unsplash.com/profile-1773680726147-06990da7c26fimage" +
                "?ixlib=rb-4.1.0&crop=faces&fit=crop&w=64&h=64"
    }
}
