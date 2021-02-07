package coil.target

import android.graphics.drawable.Drawable
import androidx.annotation.MainThread

/**
 * A listener that accepts the result of an image request.
 */
public interface Target {

    /**
     * Called when the request starts.
     */
    @MainThread
    public fun onStart(placeholder: Drawable?) {}

    /**
     * Called if an error occurs while executing the request.
     */
    @MainThread
    public fun onError(error: Drawable?) {}

    /**
     * Called if the request completes successfully.
     */
    @MainThread
    public fun onSuccess(result: Drawable) {}
}
