package coil3.target

import coil3.Image

/**
 * A callback that accepts an image.
 */
interface Target {

    /**
     * Called when the request starts.
     */
    fun onStart(placeholder: Image?, crossfadeBetweenImages: Boolean = false) {}

    /**
     * Called if an error occurs while executing the request.
     */
    fun onError(error: Image?) {}

    /**
     * Called if the request completes successfully.
     */
    fun onSuccess(result: Image) {}
}
