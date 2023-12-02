package coil3.intercept

import android.graphics.drawable.ColorDrawable
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.request.ImageResult
import coil3.request.SuccessResult

class FakeInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        return SuccessResult(
            image = ColorDrawable().asCoilImage(),
            request = chain.request,
            dataSource = DataSource.MEMORY
        )
    }
}
