@file:Suppress("unused")

package coil.fetch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.decode.Options
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Size
import kotlin.math.roundToInt

/**
 * A [PdfPageFetcher] that supports fetching and rendering a PDF page from a [File].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PdfPageFileFetcher(context: Context) : PdfPageFetcher<File>(context) {

    override fun key(data: File) = "${data.path}:${data.lastModified()}"

    override fun handles(data: File): Boolean {
        val fileName = data.name
        return fileName.endsWith(PDF_FILE_EXTENSION, true)
    }

    override fun openParcelFileDescriptor(data: File) =
        ParcelFileDescriptor.open(data, ParcelFileDescriptor.MODE_READ_ONLY)
}

/**
 * A [PdfPageFetcher] that supports fetching and rendering a PDF page from a [Uri].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PdfPageUriFetcher(private val context: Context) : PdfPageFetcher<Uri>(context) {

    override fun key(data: Uri) = data.toString()

    override fun handles(data: Uri): Boolean {
        val fileName = data.lastPathSegment
        return fileName != null && fileName.endsWith(PDF_FILE_EXTENSION, true)
    }

    override fun openParcelFileDescriptor(data: Uri) = context.contentResolver.openFileDescriptor(data, "r")
}

/**
 * A [Fetcher] that uses [PdfRenderer] to fetch and render a page from a PDF.
 *
 * Due to [PdfRenderer] requiring non-sequential reads into the data source it's not possible to make this a [Decoder].
 * Use the [PdfPageFileFetcher] and [PdfPageUriFetcher] implementations.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
abstract class PdfPageFetcher<T : Any>(private val context: Context) : Fetcher<T> {

    protected abstract fun openParcelFileDescriptor(data: T): ParcelFileDescriptor

    override suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult {
        val pfd = openParcelFileDescriptor(data)
        PdfRenderer(pfd).use { renderer ->
            val pageIndex = options.parameters.pdfPageIndex() ?: 0
            renderer.openPage(pageIndex).use { page ->
                val srcWidth = page.width
                val srcHeight = page.height
                val dstSize = when (size) {
                    is PixelSize -> {
                        if (srcWidth > 0 && srcHeight > 0) {
                            val rawScale = DecodeUtils.computeSizeMultiplier(
                                srcWidth = srcWidth,
                                srcHeight = srcHeight,
                                dstWidth = size.width,
                                dstHeight = size.height,
                                scale = options.scale
                            )
                            val scale = if (options.allowInexactSize) {
                                rawScale.coerceAtMost(1.0)
                            } else {
                                rawScale
                            }
                            val width = (scale * srcWidth).roundToInt()
                            val height = (scale * srcHeight).roundToInt()
                            PixelSize(width, height)
                        } else {
                            OriginalSize
                        }
                    }
                    is OriginalSize -> OriginalSize
                }
                val bitmap = if (dstSize is PixelSize) {
                    pool.getDirty(dstSize.width, dstSize.height, Bitmap.Config.ARGB_8888)
                } else {
                    pool.getDirty(srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
                }
                val backgroundColor = options.parameters.pdfBackgroundColor() ?: Color.WHITE
                bitmap.eraseColor(backgroundColor)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val isSampled = dstSize is PixelSize
                    && (dstSize.width < srcWidth || dstSize.height < srcHeight)
                return DrawableResult(
                    drawable = bitmap.toDrawable(context.resources),
                    isSampled = isSampled,
                    dataSource = DataSource.DISK
                )
            }
        }
    }

    companion object {
        @JvmField internal val PDF_FILE_EXTENSION = ".pdf"

        const val PDF_BACKGROUND_COLOR_KEY = "coil#pdf_background_color"
        const val PDF_PAGE_INDEX_KEY = "coil#pdf_page_index"
    }
}
