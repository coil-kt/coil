package coil.target

import android.graphics.drawable.Drawable
import androidx.annotation.MainThread

/**
 * A listener that accepts the result of an image request.
 */
interface Target {

    /**
     * Called when the request starts.
     */
    @MainThread
    fun onStart(placeholder: Drawable?) {}

    /**
     * Called if an error occurs while executing the request.
     */
    @MainThread
    fun onError(error: Drawable?) {}

    /**
     * Called if the request completes successfully.
     */
    @MainThread
    fun onSuccess(result: Drawable) {}
}
