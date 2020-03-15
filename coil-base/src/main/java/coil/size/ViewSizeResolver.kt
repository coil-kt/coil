package coil.size

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A [SizeResolver] that measures the size of a [View]. */
interface ViewSizeResolver<T : View> : SizeResolver {

    companion object {
        /**
         * Create a [ViewSizeResolver] using the default [View] measurement implementation.
         *
         * @param view The [View] to measure.
         * @param subtractPadding If true, the [view]'s padding will be subtracted from its size.
         */
        @JvmStatic
        @JvmName("create")
        operator fun <T : View> invoke(
            view: T,
            subtractPadding: Boolean = true
        ): ViewSizeResolver<T> {
            return object : ViewSizeResolver<T> {
                override val view = view
                override val subtractPadding = subtractPadding
            }
        }
    }

    /** The [View] to measure. */
    val view: T

    /** If true, the [view]'s padding will be subtracted from its size. */
    val subtractPadding: Boolean
        get() = true

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
            paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0,
            isLayoutRequested = isLayoutRequested,
            isWidth = true
        )
    }

    private fun getHeight(isLayoutRequested: Boolean): Int {
        return getDimension(
            paramSize = view.layoutParams?.height ?: -1,
            viewSize = view.height,
            paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0,
            isLayoutRequested = isLayoutRequested,
            isWidth = false
        )
    }

    /** Modified from Glide. */
    private fun getDimension(
        paramSize: Int,
        viewSize: Int,
        paddingSize: Int,
        isLayoutRequested: Boolean,
        isWidth: Boolean
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
            return view.context.resources.displayMetrics.run { if (isWidth) widthPixels else heightPixels }
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
