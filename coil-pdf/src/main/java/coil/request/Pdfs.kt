@file:Suppress("unused")
@file:JvmName("Pdfs")

package coil.request

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import coil.fetch.PdfPageFetcher
import coil.fetch.PdfPageFetcher.Companion.PDF_BACKGROUND_COLOR_KEY
import coil.fetch.PdfPageFetcher.Companion.PDF_PAGE_INDEX_KEY
import coil.request.ImageRequest
import coil.request.Parameters

/**
 * Set the background color to render a PDF on.
 *
 * Default: [android.graphics.Color.WHITE]
 *
 * @see PdfPageFetcher
 */
fun ImageRequest.Builder.pdfBackgroundColor(@ColorInt backgroundColor: Int): ImageRequest.Builder {
    return setParameter(PDF_BACKGROUND_COLOR_KEY, backgroundColor)
}

/**
 * Set the index of the page to render from a PDF.
 *
 * Default: 0
 *
 * @see PdfPageFetcher
 */
fun ImageRequest.Builder.pdfPageIndex(@IntRange(from = 0) pageIndex: Int): ImageRequest.Builder {
    require(pageIndex >= 0) { "pageIndex must be >= 0." }
    return setParameter(PDF_PAGE_INDEX_KEY, pageIndex)
}

/**
 * Get the background color to render a PDF on.
 *
 * @see PdfPageFetcher
 */
@ColorInt
fun Parameters.pdfBackgroundColor(): Int? = value(PDF_BACKGROUND_COLOR_KEY) as Int?

/**
 * Get the index of the page to render from a PDF.
 *
 * @see PdfPageFetcher
 */
@IntRange(from = 0)
fun Parameters.pdfPageIndex(): Int? = value(PDF_PAGE_INDEX_KEY) as Int?
