package coil.size

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import coil.util.log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * A [SizeResolver] that measures the size of a [View].
 */
interface ViewSizeResolver<T : View> : SizeResolver {

    companion object {
        private const val TAG = "ViewSizeResolver"

        /**
         * Construct a [ViewSizeResolver] instance using the default [View] measurement implementation.
         */
        operator fun <T : View> invoke(view: T): ViewSizeResolver<T> {
            return object : ViewSizeResolver<T> {
                override val view = view
            }
        }
    }

    val view: T

    override suspend fun size(): Size {
        // Fast path: we don't need to wait for the view to be measured.
        val isLayoutRequested = view.isLayoutRequested
        val width = getWidth(isLayoutRequested)
        val height = getHeight(isLayoutRequested)
        if (width > 0 && height > 0) {
            return PixelSize(width, height)
        }

        // Wait for the view to be measured.
        return suspendCancellableCoroutine { continuation ->
            val viewTreeObserver = view.viewTreeObserver

            val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                private var isResumed = false

                override fun onPreDraw(): Boolean {
                    if (!isResumed) {
                        isResumed = true

                        viewTreeObserver.removePreDrawListenerSafe(this)
                        val size = PixelSize(
                            width = getWidth(false).coerceAtLeast(1),
                            height = getHeight(false).coerceAtLeast(1)
                        )
                        continuation.resume(size)
                    }
                    return true
                }
            }

            viewTreeObserver.addOnPreDrawListener(preDrawListener)

            continuation.invokeOnCancellation {
                viewTreeObserver.removePreDrawListenerSafe(preDrawListener)
            }
        }
    }

    private fun getWidth(isLayoutRequested: Boolean): Int {
        return getDimension(
            paramSize = view.layoutParams?.width ?: -1,
            viewSize = view.width,
            paddingSize = view.paddingLeft + view.paddingRight,
            isLayoutRequested = isLayoutRequested
        )
    }

    private fun getHeight(isLayoutRequested: Boolean): Int {
        return getDimension(
            paramSize = view.layoutParams?.height ?: -1,
            viewSize = view.height,
            paddingSize = view.paddingTop + view.paddingBottom,
            isLayoutRequested = isLayoutRequested
        )
    }

    /** Modified from Glide. */
    private fun getDimension(
        paramSize: Int,
        viewSize: Int,
        paddingSize: Int,
        isLayoutRequested: Boolean
    ): Int {
        // Assume the dimension will match the value in the View's layout params.
        val insetParamSize = paramSize - paddingSize
        if (insetParamSize > 0) {
            return insetParamSize
        }

        // Fallback to the View's current size.
        val insetViewSize = viewSize - paddingSize
        if (insetViewSize > 0) {
            return insetViewSize
        }

        // If the dimension is set to WRAP_CONTENT and the View is fully laid out, fallback to the size of the display.
        if (!isLayoutRequested && paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
            log(TAG, Log.INFO) { "A View's width and/or height is set to WRAP_CONTENT. Falling back to the size of the display." }
            return view.context.resources.displayMetrics.run { max(widthPixels, heightPixels) }
        }

        // Unable to resolve the dimension's size.
        return -1
    }

    private fun ViewTreeObserver.removePreDrawListenerSafe(victim: ViewTreeObserver.OnPreDrawListener) {
        if (isAlive) {
            removeOnPreDrawListener(victim)
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(victim)
        }
    }
}
