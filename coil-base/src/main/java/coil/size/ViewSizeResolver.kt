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
         * @param view The view to measure.
         * @param subtractPadding If true, the view's padding will be subtracted from its size.
         */
        @JvmStatic
        @JvmOverloads
        @JvmName("create")
        operator fun <T : View> invoke(
            view: T,
            subtractPadding: Boolean = true
        ): ViewSizeResolver<T> = RealViewSizeResolver(view, subtractPadding)
    }

    /** The [View] to measure. This field should be immutable. */
    val view: T

    /** If true, the [view]'s padding will be subtracted from its size. */
    val subtractPadding: Boolean get() = true

    override suspend fun size(): Size {
        // Fast path: the view is already measured.
        getSize()?.let { return it }

        // Slow path: wait for the view to be measured.
        return suspendCancellableCoroutine { continuation ->
            val viewTreeObserver = view.viewTreeObserver

            val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                private var isResumed = false

                override fun onPreDraw(): Boolean {
                    val size = getSize()
                    if (size != null) {
                        viewTreeObserver.removePreDrawListenerSafe(this)

                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(size)
                        }
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

    private fun getSize(): PixelSize? {
        val width = getWidth().also { if (it <= 0) return null }
        val height = getHeight().also { if (it <= 0) return null }
        return PixelSize(width, height)
    }

    private fun getWidth(): Int {
        return getDimension(
            paramSize = view.layoutParams?.width ?: -1,
            viewSize = view.width,
            paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0,
            isWidth = true
        )
    }

    private fun getHeight(): Int {
        return getDimension(
            paramSize = view.layoutParams?.height ?: -1,
            viewSize = view.height,
            paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0,
            isWidth = false
        )
    }

    private fun getDimension(
        paramSize: Int,
        viewSize: Int,
        paddingSize: Int,
        isWidth: Boolean
    ): Int {
        // Assume the dimension will match the value in the view's layout params.
        val insetParamSize = paramSize - paddingSize
        if (insetParamSize > 0) {
            return insetParamSize
        }

        // Fallback to the view's current size.
        val insetViewSize = viewSize - paddingSize
        if (insetViewSize > 0) {
            return insetViewSize
        }

        // If the dimension is set to WRAP_CONTENT, fall back to the size of the display.
        if (paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
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
