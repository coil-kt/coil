package coil.annotation

/**
 * Marks builder classes that are part of a DSL.
 * This restricts calling an outer scope if it is also marked by [DslMarker].
 */
@DslMarker
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class BuilderMarker
