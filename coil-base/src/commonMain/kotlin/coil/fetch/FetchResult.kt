package coil.fetch

import android.graphics.drawable.Drawable
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.ImageSource
import okio.BufferedSource

/** The result of [Fetcher.fetch]. */
sealed class FetchResult

/**
 * An [ImageSource] result, which will be consumed by the relevant [Decoder].
 *
 * @param source The [ImageSource] to read from.
 * @param mimeType An optional MIME type for the [source].
 * @param dataSource The source that [source] was fetched from.
 */
class SourceResult(
    val source: ImageSource,
    val mimeType: String?,
    val dataSource: DataSource,
) : FetchResult() {

    fun copy(
        source: ImageSource = this.source,
        mimeType: String? = this.mimeType,
        dataSource: DataSource = this.dataSource,
    ) = SourceResult(
        source = source,
        mimeType = mimeType,
        dataSource = dataSource
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SourceResult &&
            source == other.source &&
            mimeType == other.mimeType &&
            dataSource == other.dataSource
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }
}

/**
 * A direct [Drawable] result. Return this from a [Fetcher] if its data cannot
 * be converted into a [BufferedSource].
 *
 * @param drawable The fetched [Drawable].
 * @param isSampled 'true' if [drawable] is sampled (i.e. loaded into memory
 *  at less than its original size).
 * @param dataSource The source that [drawable] was fetched from.
 */
class DrawableResult(
    val drawable: Drawable,
    val isSampled: Boolean,
    val dataSource: DataSource,
) : FetchResult() {

    fun copy(
        drawable: Drawable = this.drawable,
        isSampled: Boolean = this.isSampled,
        dataSource: DataSource = this.dataSource,
    ) = DrawableResult(
        drawable = drawable,
        isSampled = isSampled,
        dataSource = dataSource
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DrawableResult &&
            drawable == other.drawable &&
            isSampled == other.isSampled &&
            dataSource == other.dataSource
    }

    override fun hashCode(): Int {
        var result = drawable.hashCode()
        result = 31 * result + isSampled.hashCode()
        result = 31 * result + dataSource.hashCode()
        return result
    }
}
