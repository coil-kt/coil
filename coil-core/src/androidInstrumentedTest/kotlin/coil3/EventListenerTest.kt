package coil3

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.activityScenarioRule
import coil3.core.test.R
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.request.target
import coil3.request.transformations
import coil3.request.transitionFactory
import coil3.size.Size
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.activity
import coil3.test.utils.context
import coil3.transform.Transformation
import coil3.transition.Transition
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EventListenerTest {

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

    @Before
    fun before() {
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun basic() = runTest {
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

        imageLoader.testEnqueue {
            data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
        }

        eventListener.complete()
    }

    @Test
    fun transformations() = runTest {
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

        imageLoader.testEnqueue {
            data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
            transformations(object : Transformation() {
                override val cacheKey = "test_transformation"

                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                    transformationIsCalled = true
                    return input
                }
            })
        }

        assertTrue(transformationIsCalled)
        eventListener.complete()
    }

    @Test
    fun transitions() = runTest {
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

        imageLoader.testEnqueue {
            data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
            transitionFactory { target, result ->
                Transition {
                    transitionIsCalled = true
                    when (result) {
                        is SuccessResult -> target.onSuccess(result.image)
                        is ErrorResult -> target.onError(result.image)
                    }
                }
            }
        }

        assertTrue(transitionIsCalled)
        eventListener.complete()
    }

    @Test
    fun error() = runTest {
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

        try {
            imageLoader.testEnqueue {
                data("fake_data")
            }
        } catch (_: Exception) {}

        eventListener.complete()
    }

    @Test
    fun nullData() = runTest {
        val eventListener = TestEventListener(
            onStart = MethodChecker(false),
            resolveSizeStart = MethodChecker(false),
            resolveSizeEnd = MethodChecker(false),
            mapStart = MethodChecker(false),
            mapEnd = MethodChecker(false),
            keyStart = MethodChecker(false),
            keyEnd = MethodChecker(false),
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

        try {
            imageLoader.testEnqueue {
                data(null)
            }
        } catch (_: Exception) {}

        eventListener.complete()
    }

    private suspend fun ImageLoader.testEnqueue(
        builder: ImageRequest.Builder.() -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        val request = ImageRequest.Builder(context)
            .size(100, 100)
            .target(activityRule.scenario.activity.imageView)
            .listener(
                onSuccess = { _, _ -> continuation.resume(Unit) },
                onError = { _, result -> continuation.resumeWithException(result.throwable) },
                onCancel = { continuation.resumeWithException(CancellationException()) }
            )
            .apply(builder)
            .build()
        enqueue(request)
    }

    private class MethodChecker(private val callExpected: Boolean) {

        private val callCount = AtomicInteger(0)

        fun call() {
            callCount.getAndIncrement()
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
        val onStart: MethodChecker = MethodChecker(
            true
        ),
        val resolveSizeStart: MethodChecker = MethodChecker(
            true
        ),
        val resolveSizeEnd: MethodChecker = MethodChecker(
            true
        ),
        val mapStart: MethodChecker = MethodChecker(
            true
        ),
        val mapEnd: MethodChecker = MethodChecker(
            true
        ),
        val keyStart: MethodChecker = MethodChecker(
            true
        ),
        val keyEnd: MethodChecker = MethodChecker(
            true
        ),
        val fetchStart: MethodChecker = MethodChecker(
            true
        ),
        val fetchEnd: MethodChecker = MethodChecker(
            true
        ),
        val decodeStart: MethodChecker = MethodChecker(
            true
        ),
        val decodeEnd: MethodChecker = MethodChecker(
            true
        ),
        val transformStart: MethodChecker = MethodChecker(
            true
        ),
        val transformEnd: MethodChecker = MethodChecker(
            true
        ),
        val transitionStart: MethodChecker = MethodChecker(
            true
        ),
        val transitionEnd: MethodChecker = MethodChecker(
            true
        ),
        val onSuccess: MethodChecker = MethodChecker(
            true
        ),
        val onCancel: MethodChecker = MethodChecker(
            true
        ),
        val onError: MethodChecker = MethodChecker(
            true
        )
    ) : EventListener() {

        override fun onStart(request: ImageRequest) = onStart.call()
        override fun resolveSizeStart(request: ImageRequest) = resolveSizeStart.call()
        override fun resolveSizeEnd(request: ImageRequest, size: Size) = resolveSizeEnd.call()
        override fun mapStart(request: ImageRequest, input: Any) = mapStart.call()
        override fun mapEnd(request: ImageRequest, output: Any) = mapEnd.call()
        override fun keyStart(request: ImageRequest, input: Any) = keyStart.call()
        override fun keyEnd(request: ImageRequest, output: String?) = keyEnd.call()
        override fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options) = fetchStart.call()
        override fun fetchEnd(request: ImageRequest, fetcher: Fetcher, options: Options, result: FetchResult?) = fetchEnd.call()
        override fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) = decodeStart.call()
        override fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult?) = decodeEnd.call()
        override fun transformStart(request: ImageRequest, input: Bitmap) = transformStart.call()
        override fun transformEnd(request: ImageRequest, output: Bitmap) = transformEnd.call()
        override fun transitionStart(request: ImageRequest, transition: Transition) = transitionStart.call()
        override fun transitionEnd(request: ImageRequest, transition: Transition) = transitionEnd.call()
        override fun onCancel(request: ImageRequest) = onCancel.call()
        override fun onError(request: ImageRequest, result: ErrorResult) = onError.call()
        override fun onSuccess(request: ImageRequest, result: SuccessResult) = onSuccess.call()

        fun complete() {
            onStart.complete("onStart")
            resolveSizeStart.complete("resolveSizeStart")
            resolveSizeEnd.complete("resolveSizeEnd")
            mapStart.complete("mapStart")
            mapEnd.complete("mapEnd")
            keyStart.complete("keyStart")
            keyEnd.complete("keyEnd")
            fetchStart.complete("fetchStart")
            fetchEnd.complete("fetchEnd")
            decodeStart.complete("decodeStart")
            decodeEnd.complete("decodeEnd")
            transformStart.complete("transformStart")
            transformEnd.complete("transformEnd")
            transitionStart.complete("transitionStart")
            transitionEnd.complete("transitionEnd")
            onCancel.complete("onCancel")
            onError.complete("onError")
            onSuccess.complete("onSuccess")
        }
    }
}
