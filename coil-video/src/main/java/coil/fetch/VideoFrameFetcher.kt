@file:Suppress("unused")

package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.decode.DataSource
import coil.decode.Options
import coil.decode.VideoFrameDecoder
import coil.decode.VideoFrameDecoderDelegate
import coil.size.Size
import java.io.File

/**
 * A [VideoFrameFetcher] that supports fetching and decoding a video frame from a [File].
 */
class VideoFrameFileFetcher(context: Context) : VideoFrameFetcher<File>(context) {

    override fun key(data: File) = "${data.path}:${data.lastModified()}"

    override fun handles(data: File): Boolean {
        val fileName = data.name
        return SUPPORTED_FILE_EXTENSIONS.any { fileName.endsWith(it, true) }
    }

    override fun MediaMetadataRetriever.setDataSource(data: File) = setDataSource(data.path)
}

/**
 * A [VideoFrameFetcher] that supports fetching and decoding a video frame from a [Uri].
 */
class VideoFrameUriFetcher(private val context: Context) : VideoFrameFetcher<Uri>(context) {

    override fun key(data: Uri) = data.toString()

    override fun handles(data: Uri): Boolean {
        val fileName = data.lastPathSegment
        return fileName != null &&
            data.scheme !in UNSUPPORTED_SCHEMES &&
            SUPPORTED_FILE_EXTENSIONS.any { fileName.endsWith(it, true) }
    }

    override fun MediaMetadataRetriever.setDataSource(data: Uri) {
        if (data.scheme == ContentResolver.SCHEME_FILE && data.pathSegments.firstOrNull() == ASSET_FILE_PATH_ROOT) {
            // Work around setDataSource(Context, Uri) not handling android_asset uris properly.
            val path = data.pathSegments.drop(1).joinToString("/")
            context.assets.openFd(path).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        } else {
            setDataSource(context, data)
        }
    }
}

/**
 * A [Fetcher] that uses [MediaMetadataRetriever] to fetch and decode a frame from a video.
 */
abstract class VideoFrameFetcher<T : Any>(context: Context) : Fetcher<T> {

    private val delegate = VideoFrameDecoderDelegate(context)

    protected abstract fun MediaMetadataRetriever.setDataSource(data: T)

    override suspend fun fetch(
        data: T,
        size: Size,
        options: Options
    ): FetchResult {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(data)
            val (drawable, isSampled) = delegate.decode(retriever, size, options)
            return DrawableResult(
                drawable = drawable,
                isSampled = isSampled,
                dataSource = DataSource.DISK
            )
        } finally {
            retriever.release()
        }
    }

    companion object {
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        @JvmField internal val SUPPORTED_FILE_EXTENSIONS = arrayOf(".3gp", ".mkv", ".mp4", ".ts", ".webm")

        @JvmField internal val UNSUPPORTED_SCHEMES = arrayOf("http", "https")

        internal const val ASSET_FILE_PATH_ROOT = "android_asset"

        const val VIDEO_FRAME_MICROS_KEY = VideoFrameDecoder.VIDEO_FRAME_MICROS_KEY
        const val VIDEO_FRAME_OPTION_KEY = VideoFrameDecoder.VIDEO_FRAME_OPTION_KEY
    }
}
