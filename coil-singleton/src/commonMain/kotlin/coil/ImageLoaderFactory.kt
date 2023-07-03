package coil

/**
 * A factory that creates new [ImageLoader] instances.
 *
 * To configure how the singleton [ImageLoader] is created **either**:
 * - Implement [ImageLoaderFactory] on your Android `Application` class.
 * - **Or** call [Coil.setImageLoader] with your [ImageLoaderFactory].
 */
fun interface ImageLoaderFactory {

    /**
     * Return a new [ImageLoader].
     */
    fun newImageLoader(): ImageLoader
}
