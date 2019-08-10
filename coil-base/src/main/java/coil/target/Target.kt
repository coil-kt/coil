package coil.target

import android.graphics.drawable.Drawable
import androidx.annotation.MainThread

/**
 * A listener that accepts the result of an image load.
 *
 * Each lifecycle method is called at most once. [onSuccess] and [onError] are mutually exclusive.
 */
interface Target {

    /**
     * Called when the image request starts.
     */
    @MainThread
    fun onStart(placeholder: Drawable?) {}

    /**
     * Called if the image request is successful.
     */
    @MainThread
    fun onSuccess(result: Drawable) {}

    /**
     * Called if the image request fails.
     */
    @MainThread
    fun onError(error: Drawable?) {}
}
