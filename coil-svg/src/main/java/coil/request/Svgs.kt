package coil.request

import coil.Extras
import coil.getExtra

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
