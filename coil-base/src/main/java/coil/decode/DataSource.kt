package coil.decode

import android.graphics.drawable.Drawable
import coil.fetch.DrawableResult
import coil.fetch.SourceResult

/**
 * Represents the source that a [Drawable] was loaded from.
 *
 * @see SourceResult.dataSource
 * @see DrawableResult.dataSource
 */
enum class DataSource {
    MEMORY,
    DISK,
    NETWORK
}
