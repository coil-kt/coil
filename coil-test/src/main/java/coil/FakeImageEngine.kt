package coil

import android.graphics.drawable.Drawable
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.ImageResult
import coil.request.SuccessResult
import java.util.LinkedList
import java.util.Queue

class FakeImageEngine : Interceptor {

    private val results = mutableMapOf<Any, Queue<FakeImageLoader.Engine>>()
    private var default: FakeImageLoader.Engine? = null

    @Synchronized
    fun enqueue(
        data: Any,
        drawable: Drawable,
        dataSource: DataSource = DataSource.DISK,
        memoryCacheKey: MemoryCache.Key? = null,
        diskCacheKey: String? = null,
    ) = enqueue(
        data = data,
        engine = { request ->
            SuccessResult(
                drawable = drawable,
                request = request,
                dataSource = dataSource,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey
            )
        }
    )

    @Synchronized
    fun enqueue(data: Any, engine: FakeImageLoader.Engine) = apply {
        results.getOrPut(data) { LinkedList() }.offer(engine)
    }

    @Synchronized
    fun default(default: FakeImageLoader.Engine) = apply {
        this.default = default
    }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        TODO("Not yet implemented")
    }
}
