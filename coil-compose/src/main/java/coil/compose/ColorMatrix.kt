package coil.compose

import androidx.compose.ui.graphics.ColorMatrix

/**
 * Ideally we'd use [ColorMatrix.setToSaturation], however we can't since it
 * resets the matrix before applying the values.
 */
internal fun ColorMatrix.updateSaturation(saturation: Float) {
    val invSat = 1 - saturation
    val r = 0.213f * invSat
    val g = 0.715f * invSat
    val b = 0.072f * invSat
    this[0, 0] = r + saturation
    this[0, 1] = g
    this[0, 2] = b
    this[1, 0] = r
    this[1, 1] = g + saturation
    this[1, 2] = b
    this[2, 0] = r
    this[2, 1] = g
    this[2, 2] = b + saturation
}

internal fun ColorMatrix.updateBrightness(brightness: Float) {
    val darkening = (1f - brightness) * 255
    this[0, 4] = darkening
    this[1, 4] = darkening
    this[2, 4] = darkening
}

internal fun ColorMatrix.updateAlpha(alpha: Float) = set(row = 3, column = 3, v = alpha)
