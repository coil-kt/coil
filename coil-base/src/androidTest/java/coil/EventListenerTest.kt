package coil

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.base.test.R
import coil.bitmap.BitmapPool
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Size
import coil.transform.Transformation
import coil.transition.Transition
import coil.transition.TransitionTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.assertTrue

@OptIn(ExperimentalCoilApi::class)
class EventListenerTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun basic() {
        val eventListener = TestEventListener(
            transformStart = MethodChecker(false),
            transformEnd = MethodChecker(false),
            transitionStart = MethodChecker(false),
            transitionEnd = MethodChecker(false),
            onCancel = MethodChecker(false),
            onError = MethodChecker(false)
        )

        val imageLoader = ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        runBlocking {
            imageLoader.testEnqueue {
                data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
            }
        }

        eventListener.complete()
    }

    @Test
    fun transformations() {
        val eventListener = TestEventListener(
            transitionStart = MethodChecker(false),
            transitionEnd = MethodChecker(false),
            onCancel = MethodChecker(false),
            onError = MethodChecker(false)
        )

        val imageLoader = ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        var transformationIsCalled = false

        runBlocking {
            imageLoader.testEnqueue {
                data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
                transformations(object : Transformation {
                    override fun key() = "test_transformation"

                    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
                        transformationIsCalled = true
                        return input
                    }
                })
            }
        }

        assertTrue(transformationIsCalled)
        eventListener.complete()
    }

    @Test
    fun transitions() {
        val eventListener = TestEventListener(
            transformStart = MethodChecker(false),
            transformEnd = MethodChecker(false),
            onCancel = MethodChecker(false),
            onError = MethodChecker(false)
        )

        val imageLoader = ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        var transitionIsCalled = false

        runBlocking {
            imageLoader.testEnqueue {
                data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
                transition(object : Transition {
                    override suspend fun transition(target: TransitionTarget, result: ImageResult) {
                        transitionIsCalled = true
                        when (result) {
                            is SuccessResult -> target.onSuccess(result.drawable)
                            is ErrorResult -> target.onError(result.drawable)
                        }
                    }
                })
            }
        }

        assertTrue(transitionIsCalled)
        eventListener.complete()
    }

    @Test
    fun error() {
        val eventListener = TestEventListener(
            fetchStart = MethodChecker(false),
            fetchEnd = MethodChecker(false),
            decodeStart = MethodChecker(false),
            decodeEnd = MethodChecker(false),
            transformStart = MethodChecker(false),
            transformEnd = MethodChecker(false),
            transitionStart = MethodChecker(false),
            transitionEnd = MethodChecker(false),
            onSuccess = MethodChecker(false),
            onCancel = MethodChecker(false)
        )

        val imageLoader = ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        runBlocking {
            try {
                imageLoader.testEnqueue {
                    data("fake_data")
                }
            } catch (_: Exception) {}
        }

        eventListener.complete()
    }

    @Test
    fun nullData() {
        val eventListener = TestEventListener(
            onStart = MethodChecker(false),
            resolveSizeStart = MethodChecker(false),
            resolveSizeEnd = MethodChecker(false),
            mapStart = MethodChecker(false),
            mapEnd = MethodChecker(false),
            fetchStart = MethodChecker(false),
            fetchEnd = MethodChecker(false),
            decodeStart = MethodChecker(false),
            decodeEnd = MethodChecker(false),
            transformStart = MethodChecker(false),
            transformEnd = MethodChecker(false),
            transitionStart = MethodChecker(false),
            transitionEnd = MethodChecker(false),
            onSuccess = MethodChecker(false),
            onCancel = MethodChecker(false)
        )

        val imageLoader = ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        runBlocking {
            try {
                imageLoader.testEnqueue {
                    data(null)
                }
            } catch (_: Exception) {}
        }

        eventListener.complete()
    }

    private suspend fun ImageLoader.testEnqueue(
        builder: ImageRequest.Builder.() -> Unit
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val request = ImageRequest.Builder(context)
            .size(100, 100)
            .target(ImageView(context))
            .listener(
                onSuccess = { _, _ -> continuation.resume(Unit) },
                onError = { _, throwable -> continuation.resumeWithException(throwable) },
                onCancel = { continuation.resumeWithException(CancellationException()) }
            )
            .apply(builder)
            .build()
        enqueue(request)
    }

    private class MethodChecker(private val callExpected: Boolean) {

        private val callCount = AtomicInteger(0)

        fun call() {
            callCount.incrementAndGet()
        }

        fun complete(eventName: String) {
            val count = callCount.get()
            require(count in 0..1) { "$eventName was called $count times." }

            if (callExpected) {
                require(count == 1) { "$eventName WAS NOT called." }
            } else {
                require(count == 0) { "$eventName WAS called." }
            }
        }
    }

    private class TestEventListener(
        val onStart: MethodChecker = MethodChecker(true),
        val resolveSizeStart: MethodChecker = MethodChecker(true),
        val resolveSizeEnd: MethodChecker = MethodChecker(true),
        val mapStart: MethodChecker = MethodChecker(true),
        val mapEnd: MethodChecker = MethodChecker(true),
        val fetchStart: MethodChecker = MethodChecker(true),
        val fetchEnd: MethodChecker = MethodChecker(true),
        val decodeStart: MethodChecker = MethodChecker(true),
        val decodeEnd: MethodChecker = MethodChecker(true),
        val transformStart: MethodChecker = MethodChecker(true),
        val transformEnd: MethodChecker = MethodChecker(true),
        val transitionStart: MethodChecker = MethodChecker(true),
        val transitionEnd: MethodChecker = MethodChecker(true),
        val onSuccess: MethodChecker = MethodChecker(true),
        val onCancel: MethodChecker = MethodChecker(true),
        val onError: MethodChecker = MethodChecker(true)
    ) : EventListener {

        override fun onStart(request: ImageRequest) = onStart.call()
        override fun resolveSizeStart(request: ImageRequest) = resolveSizeStart.call()
        override fun resolveSizeEnd(request: ImageRequest, size: Size) = resolveSizeEnd.call()
        override fun mapStart(request: ImageRequest, input: Any) = mapStart.call()
        override fun mapEnd(request: ImageRequest, output: Any) = mapEnd.call()
        override fun fetchStart(request: ImageRequest, fetcher: Fetcher<*>, options: Options) = fetchStart.call()
        override fun fetchEnd(request: ImageRequest, fetcher: Fetcher<*>, options: Options, result: FetchResult) = fetchEnd.call()
        override fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) = decodeStart.call()
        override fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult) = decodeEnd.call()
        override fun transformStart(request: ImageRequest, input: Bitmap) = transformStart.call()
        override fun transformEnd(request: ImageRequest, output: Bitmap) = transformEnd.call()
        override fun transitionStart(request: ImageRequest) = transitionStart.call()
        override fun transitionEnd(request: ImageRequest) = transitionEnd.call()
        override fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) = onSuccess.call()
        override fun onCancel(request: ImageRequest) = onCancel.call()
        override fun onError(request: ImageRequest, throwable: Throwable) = onError.call()

        fun complete() {
            onStart.complete("onStart")
            resolveSizeStart.complete("resolveSizeStart")
            resolveSizeEnd.complete("resolveSizeEnd")
            mapStart.complete("mapStart")
            mapEnd.complete("mapEnd")
            fetchStart.complete("fetchStart")
            fetchEnd.complete("fetchEnd")
            decodeStart.complete("decodeStart")
            decodeEnd.complete("decodeEnd")
            transformStart.complete("transformStart")
            transformEnd.complete("transformEnd")
            transitionStart.complete("transitionStart")
            transitionEnd.complete("transitionEnd")
            onSuccess.complete("onSuccess")
            onCancel.complete("onCancel")
            onError.complete("onError")
        }
    }
}
