package coil.intercept

import android.graphics.drawable.ColorDrawable
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.request.ImageResult
import coil.request.Metadata
import coil.request.SuccessResult

@OptIn(ExperimentalCoilApi::class)
class FakeInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        return SuccessResult(
            drawable = ColorDrawable(),
            request = chain.request,
            metadata = Metadata(
                key = null,
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        )
    }
}
