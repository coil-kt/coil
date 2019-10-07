package coil.annotation

import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Marks builder classes that are part of a DSL.
 * This restricts calling an outer scope if it is also marked by [DslMarker].
 */
@DslMarker
@Retention(SOURCE)
annotation class BuilderMarker
