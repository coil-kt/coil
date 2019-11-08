package coil.annotation

/**
 * Marks declarations that are still **experimental**.
 * Targets marked by this annotation design is incubating and may contain breaking changes in the future.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalCoil
