package coil.size

import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * A [SizeResolver] that measures the size of a [View].
 */
interface ViewSizeResolver<T : View> : SizeResolver {

    companion object {
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
        // Fast path: assume the View's height will match the data in its layout params.
        view.layoutParams?.let { layoutParams ->
            val width = layoutParams.width - view.paddingLeft - view.paddingRight
            val height = layoutParams.height - view.paddingTop - view.paddingBottom
            if (width > 0 && height > 0) {
                return PixelSize(width, height)
            }
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
                        val width = max(1, view.width - view.paddingLeft - view.paddingRight)
                        val height = max(1, view.height - view.paddingTop - view.paddingBottom)
                        continuation.resume(PixelSize(width, height))
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

    private fun ViewTreeObserver.removePreDrawListenerSafe(victim: ViewTreeObserver.OnPreDrawListener) {
        when {
            isAlive -> removeOnPreDrawListener(victim)
            else -> view.viewTreeObserver.removeOnPreDrawListener(victim)
        }
    }
}
