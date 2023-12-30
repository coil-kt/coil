package coil3.size

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import kotlin.coroutines.resume
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Create a [ViewSizeResolver] using the default [View] measurement implementation.
 *
 * @param view The view to measure.
 * @param subtractPadding If true, the view's padding will be subtracted from its size.
 */
@JvmOverloads
fun <T : View> ViewSizeResolver(
    view: T,
    subtractPadding: Boolean = true
): ViewSizeResolver<T> = RealViewSizeResolver(view, subtractPadding)

/**
 * A [SizeResolver] that measures the size of a [View].
 */
interface ViewSizeResolver<T : View> : SizeResolver {

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

            val preDrawListener = object : OnPreDrawListener {
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

    private fun getSize(): Size? {
        val width = getWidth() ?: return null
        val height = getHeight() ?: return null
        return Size(width, height)
    }

    private fun getWidth() = getDimension(
        paramSize = view.layoutParams?.width ?: -1,
        viewSize = view.width,
        paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0
    )

    private fun getHeight() = getDimension(
        paramSize = view.layoutParams?.height ?: -1,
        viewSize = view.height,
        paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0
    )

    private fun getDimension(paramSize: Int, viewSize: Int, paddingSize: Int): Dimension? {
        // If the dimension is set to WRAP_CONTENT, then the dimension is undefined.
        if (paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return Dimension.Undefined
        }

        // Assume the dimension will match the value in the view's layout params.
        val insetParamSize = paramSize - paddingSize
        if (insetParamSize > 0) {
            return Dimension(insetParamSize)
        }

        // Fallback to the view's current dimension.
        val insetViewSize = viewSize - paddingSize
        if (insetViewSize > 0) {
            return Dimension(insetViewSize)
        }

        // Unable to resolve the dimension's value.
        return null
    }

    private fun ViewTreeObserver.removePreDrawListenerSafe(victim: OnPreDrawListener) {
        if (isAlive) {
            removeOnPreDrawListener(victim)
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(victim)
        }
    }
}
