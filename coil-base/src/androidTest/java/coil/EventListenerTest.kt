package coil

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.base.test.R
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.Fetcher
import coil.request.LoadRequestBuilder
import coil.request.Request
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.util.createLoadRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            imageLoader.testLoad {
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

        runBlocking {
            imageLoader.testLoad {
                data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
                transformations(CircleCropTransformation())
            }
        }

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

        runBlocking {
            imageLoader.testLoad {
                data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
                crossfade(true)
            }
        }

        eventListener.complete()
    }

    @Test
    fun error() {
        val eventListener = TestEventListener(
            resolveSizeStart = MethodChecker(false),
            resolveSizeEnd = MethodChecker(false),
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
                imageLoader.testLoad {
                    data("fake_data")
                }
            } catch (_: Exception) {}
        }

        eventListener.complete()
    }

    private suspend fun ImageLoader.testLoad(
        builder: LoadRequestBuilder.() -> Unit
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val request = createLoadRequest(context) {
            size(100, 100)
            target(ImageView(context))
            listener(
                onSuccess = { _, _ -> continuation.resume(Unit) },
                onError = { _, throwable -> continuation.resumeWithException(throwable) },
                onCancel = { continuation.resumeWithException(CancellationException()) }
            )
            builder()
        }
        load(request)
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
        val mapStart: MethodChecker = MethodChecker(true),
        val mapEnd: MethodChecker = MethodChecker(true),
        val resolveSizeStart: MethodChecker = MethodChecker(true),
        val resolveSizeEnd: MethodChecker = MethodChecker(true),
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

        override fun onStart(request: Request) = onStart.call()
        override fun mapStart(request: Request) = mapStart.call()
        override fun mapEnd(request: Request, mappedData: Any) = mapEnd.call()
        override fun resolveSizeStart(request: Request) = resolveSizeStart.call()
        override fun resolveSizeEnd(request: Request, size: Size) = resolveSizeEnd.call()
        override fun fetchStart(request: Request, fetcher: Fetcher<*>, options: Options) = fetchStart.call()
        override fun fetchEnd(request: Request, fetcher: Fetcher<*>, options: Options) = fetchEnd.call()
        override fun decodeStart(request: Request, decoder: Decoder, options: Options) = decodeStart.call()
        override fun decodeEnd(request: Request, decoder: Decoder, options: Options) = decodeEnd.call()
        override fun transformStart(request: Request) = transformStart.call()
        override fun transformEnd(request: Request) = transformEnd.call()
        override fun transitionStart(request: Request) = transitionStart.call()
        override fun transitionEnd(request: Request) = transitionEnd.call()
        override fun onSuccess(request: Request, source: DataSource) = onSuccess.call()
        override fun onCancel(request: Request) = onCancel.call()
        override fun onError(request: Request, throwable: Throwable) = onError.call()

        fun complete() {
            onStart.complete("onStart")
            mapStart.complete("mapStart")
            mapEnd.complete("mapEnd")
            resolveSizeStart.complete("resolveSizeStart")
            resolveSizeEnd.complete("resolveSizeEnd")
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
