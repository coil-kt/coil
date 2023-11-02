package coil.target

import coil.Image
import coil.annotation.MainThread

/**
 * A callback that accepts an image.
 */
interface Target {

    /**
     * Called when the request starts.
     */
    @MainThread
    fun onStart(placeholder: Image?) {}

    /**
     * Called if an error occurs while executing the request.
     */
    @MainThread
    fun onError(error: Image?) {}

    /**
     * Called if the request completes successfully.
     */
    @MainThread
    fun onSuccess(result: Image) {}
}
