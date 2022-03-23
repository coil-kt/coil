@file:Suppress("UNCHECKED_CAST", "unused")
@file:JvmName("Svgs")

package coil.request

import coil.decode.SvgDecoder.Companion.CSS_KEY

/**
 * Specifies additional CSS rules that will be applied when rendering an SVG in addition to any
 * rules specified in the SVG itself.
 */
fun ImageRequest.Builder.css(css: String): ImageRequest.Builder {
    return setParameter(CSS_KEY, css)
}

/**
 * Get the additional CSS rules.
 */
fun Parameters.css(): String? = value(CSS_KEY)
