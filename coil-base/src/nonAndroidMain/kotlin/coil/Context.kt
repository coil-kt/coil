package coil

actual class Context private constructor() {
    companion object {
        val INSTANCE = Context()
    }
}
