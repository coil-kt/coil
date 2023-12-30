package coil3.intercept

import coil3.decode.DataSource
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.test.utils.FakeImage

class FakeEngineInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        return SuccessResult(
            image = FakeImage(),
            request = chain.request,
            dataSource = DataSource.MEMORY,
        )
    }
}
