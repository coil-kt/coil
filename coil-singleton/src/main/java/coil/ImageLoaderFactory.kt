package coil

import android.app.Application

/**
 * A factory that creates new [ImageLoader] instances.
 *
 * To configure how the singleton [ImageLoader] is created **either**:
 * - Implement [ImageLoaderFactory] in your [Application].
 * - **Or** call [Coil.setImageLoader] with your [ImageLoaderFactory].
 */
public fun interface ImageLoaderFactory {

    /**
     * Return a new [ImageLoader].
     */
    public fun newImageLoader(): ImageLoader
}
