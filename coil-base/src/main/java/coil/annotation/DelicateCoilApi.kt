package coil.annotation

/**
 * Marks declarations that are **delicate**.
 *
 * Carefully read documentation of any declaration marked as `DelicateCoilApi` to ensure you're
 * aware of any potential issues/limitations.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class DelicateCoilApi
