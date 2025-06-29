package coil3.svg.internal

import coil3.svg.Svg
import okio.BufferedSource

internal expect fun parseSvg(source: BufferedSource): Svg
