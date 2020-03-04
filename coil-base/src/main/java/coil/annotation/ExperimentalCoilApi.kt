package coil.annotation

/**
 * Marks declarations that are still **experimental**.
 * Targets marked by this annotation may contain breaking changes in the future as their design is still incubating.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "This API is experimental and may contain breaking changes in the future as its design is still incubating.",
    level = RequiresOptIn.Level.WARNING
)
annotation class ExperimentalCoilApi
