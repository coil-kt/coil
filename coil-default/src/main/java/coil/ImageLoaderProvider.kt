package coil

import android.app.Application

/**
 * A class that can provide the default [ImageLoader].
 *
 * Either implement [ImageLoaderProvider] on your [Application] **or** call [Coil.setImageLoader]
 * with your [ImageLoaderProvider] to provide the default [ImageLoader].
 */
interface ImageLoaderProvider {

    /**
     * Return the default [ImageLoader].
     */
    fun getImageLoader(): ImageLoader
}
