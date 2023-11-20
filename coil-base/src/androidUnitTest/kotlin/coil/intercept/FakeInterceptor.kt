package coil.intercept

import android.graphics.drawable.ColorDrawable
import coil.asCoilImage
import coil.decode.DataSource
import coil.request.ImageResult
import coil.request.SuccessResult

class FakeInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        return SuccessResult(
            image = ColorDrawable().asCoilImage(),
            request = chain.request,
            dataSource = DataSource.MEMORY
        )
    }
}
