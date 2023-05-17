package coil.annotation

/**
 * Marks declarations that are still **experimental**.
 *
 * Targets marked by this annotation may contain breaking changes in the future as their design
 * is still incubating.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalCoilApi

/**
 * Marks declarations that have their visibility relaxed to make code easier to test.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class VisibleForTesting

/**
 * Marks declarations that should only be called from the main thread.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class MainThread

/**
 * Marks declarations that should only be called from a worker thread (on platforms that have
 * multiple threads).
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class WorkerThread
