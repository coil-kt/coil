package coil.target

import coil.Image
import coil.annotation.MainThread

/**
 * A listener that accepts the result of an image request.
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
