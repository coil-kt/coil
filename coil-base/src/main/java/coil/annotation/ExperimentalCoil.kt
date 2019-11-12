package coil.annotation

/**
 * Marks declarations that are still **experimental**.
 * Targets marked by this annotation may contain breaking changes in the future as their design is still incubating.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalCoil
