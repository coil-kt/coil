package coil3

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil3.base.test.R
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
import coil3.transform.Transformation
import coil3.transition.Transition
import coil3.test.ViewTestActivity
import coil3.test.activity
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

    private lateinit var context: Context

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun basic() = runTest {
        val eventListener = coil3.EventListenerTest.TestEventListener(
            transformStart = coil3.EventListenerTest.MethodChecker(false),
            transformEnd = coil3.EventListenerTest.MethodChecker(false),
            transitionStart = coil3.EventListenerTest.MethodChecker(false),
            transitionEnd = coil3.EventListenerTest.MethodChecker(false),
            onCancel = coil3.EventListenerTest.MethodChecker(false),
            onError = coil3.EventListenerTest.MethodChecker(false)
        )

        val imageLoader = coil3.ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        imageLoader.testEnqueue {
            data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
        }

        eventListener.complete()
    }

    @Test
    fun transformations() = runTest {
        val eventListener = coil3.EventListenerTest.TestEventListener(
            transitionStart = coil3.EventListenerTest.MethodChecker(false),
            transitionEnd = coil3.EventListenerTest.MethodChecker(false),
            onCancel = coil3.EventListenerTest.MethodChecker(false),
            onError = coil3.EventListenerTest.MethodChecker(false)
        )

        val imageLoader = coil3.ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        var transformationIsCalled = false

        imageLoader.testEnqueue {
            data("$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}")
            transformations(object : Transformation {
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
        val eventListener = coil3.EventListenerTest.TestEventListener(
            transformStart = coil3.EventListenerTest.MethodChecker(false),
            transformEnd = coil3.EventListenerTest.MethodChecker(false),
            onCancel = coil3.EventListenerTest.MethodChecker(false),
            onError = coil3.EventListenerTest.MethodChecker(false)
        )

        val imageLoader = coil3.ImageLoader.Builder(context)
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
        val eventListener = coil3.EventListenerTest.TestEventListener(
            fetchStart = coil3.EventListenerTest.MethodChecker(false),
            fetchEnd = coil3.EventListenerTest.MethodChecker(false),
            decodeStart = coil3.EventListenerTest.MethodChecker(false),
            decodeEnd = coil3.EventListenerTest.MethodChecker(false),
            transformStart = coil3.EventListenerTest.MethodChecker(false),
            transformEnd = coil3.EventListenerTest.MethodChecker(false),
            transitionStart = coil3.EventListenerTest.MethodChecker(false),
            transitionEnd = coil3.EventListenerTest.MethodChecker(false),
            onSuccess = coil3.EventListenerTest.MethodChecker(false),
            onCancel = coil3.EventListenerTest.MethodChecker(false)
        )

        val imageLoader = coil3.ImageLoader.Builder(context)
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
        val eventListener = coil3.EventListenerTest.TestEventListener(
            onStart = coil3.EventListenerTest.MethodChecker(false),
            resolveSizeStart = coil3.EventListenerTest.MethodChecker(false),
            resolveSizeEnd = coil3.EventListenerTest.MethodChecker(false),
            mapStart = coil3.EventListenerTest.MethodChecker(false),
            mapEnd = coil3.EventListenerTest.MethodChecker(false),
            keyStart = coil3.EventListenerTest.MethodChecker(false),
            keyEnd = coil3.EventListenerTest.MethodChecker(false),
            fetchStart = coil3.EventListenerTest.MethodChecker(false),
            fetchEnd = coil3.EventListenerTest.MethodChecker(false),
            decodeStart = coil3.EventListenerTest.MethodChecker(false),
            decodeEnd = coil3.EventListenerTest.MethodChecker(false),
            transformStart = coil3.EventListenerTest.MethodChecker(false),
            transformEnd = coil3.EventListenerTest.MethodChecker(false),
            transitionStart = coil3.EventListenerTest.MethodChecker(false),
            transitionEnd = coil3.EventListenerTest.MethodChecker(false),
            onSuccess = coil3.EventListenerTest.MethodChecker(false),
            onCancel = coil3.EventListenerTest.MethodChecker(false)
        )

        val imageLoader = coil3.ImageLoader.Builder(context)
            .eventListener(eventListener)
            .build()

        try {
            imageLoader.testEnqueue {
                data(null)
            }
        } catch (_: Exception) {}

        eventListener.complete()
    }

    private suspend fun coil3.ImageLoader.testEnqueue(
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
        val onStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val resolveSizeStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val resolveSizeEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val mapStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val mapEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val keyStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val keyEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val fetchStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val fetchEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val decodeStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val decodeEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val transformStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val transformEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val transitionStart: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val transitionEnd: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val onSuccess: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val onCancel: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        ),
        val onError: coil3.EventListenerTest.MethodChecker = coil3.EventListenerTest.MethodChecker(
            true
        )
    ) : coil3.EventListener() {

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
