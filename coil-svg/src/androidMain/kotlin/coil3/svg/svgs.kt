package coil3.svg

import coil3.Extras
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options

/**
 * Specifies additional CSS rules that will be applied when rendering an SVG in addition to any
 * rules specified in the SVG itself.
 */
fun ImageRequest.Builder.css(css: String?) = apply {
    memoryCacheKeyExtra("coil#css", css)
    extras[cssKey] = css
}

val ImageRequest.css: String?
    get() = getExtra(cssKey)

val Options.css: String?
    get() = getExtra(cssKey)

val Extras.Key.Companion.css: Extras.Key<String?>
    get() = cssKey

private val cssKey = Extras.Key<String?>(default = null)
