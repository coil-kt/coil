package coil.target

import android.graphics.Bitmap
import android.view.View
import coil.base.R

/**
 * A [ViewTarget] that supports [Bitmap] pooling.
 *
 * [PoolableViewTarget] has a simple implementation of [bitmap] so consumers only need to implement [onClear].
 * See [PoolableTarget] for the behaviour requirements for bitmap pooling.
 *
 * @see PoolableTarget
 * @see ViewTarget
 * @see ImageViewTarget
 */
interface PoolableViewTarget<T : View> : PoolableTarget, ViewTarget<T> {

    override var bitmap: Bitmap?
        get() = view.getTag(R.id.coil_bitmap) as? Bitmap
        set(value) = view.setTag(R.id.coil_bitmap, value)
}
