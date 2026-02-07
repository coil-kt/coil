package coil3.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class ErrorFunctionDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = ErrorFunctionDetector()

    override fun getIssues(): List<Issue> = listOf(ErrorFunctionDetector.ISSUE)

    private val imageViewStub: TestFile = kotlin(
        """
        package android.widget
        class ImageView
        """,
    ).indented()

    private val coilExtensionsStub: TestFile = kotlin(
        """
        package coil3
        import android.widget.ImageView
        inline fun ImageView.load(
            data: Any?,
            builder: ImageRequestBuilder.() -> Unit = {},
        ) {}
        class ImageRequestBuilder {
            fun placeholder(drawable: Int) {}
            fun crossfade(enable: Boolean) {}
        }
        """,
    ).indented()

    private val coilErrorExtensionStub: TestFile = kotlin(
        """
        package coil3.request
        import coil3.ImageRequestBuilder
        fun ImageRequestBuilder.error(drawable: Int) {}
        """,
    ).indented()

    private val kotlinErrorStub: TestFile = kotlin(
        """
        package kotlin
        fun error(message: Any): Nothing = throw IllegalStateException(message.toString())
        """,
    ).indented()

    @Test
    fun testStdlibErrorInsideLoadBlockTriggersWarning() {
        lint()
            .allowMissingSdk()
            .testModes(TestMode.JVM_OVERLOADS)
            .files(
                imageViewStub,
                coilExtensionsStub,
                kotlinErrorStub,
                kotlin(
                    """
                    package test

                    import android.widget.ImageView
                    import coil3.load
                    import kotlin.error

                    fun test(imageView: ImageView) {
                        imageView.load("https://example.com/image.jpg") {
                            error("Failed to load") // Wrong: This is kotlin.error()
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectWarningCount(1)
            .expectContains("Using kotlin.error() inside ImageRequest.Builder")
    }

    @Test
    fun testCoilErrorExtensionDoesNotTriggerWarning() {
        lint()
            .allowMissingSdk()
            .files(
                imageViewStub,
                coilExtensionsStub,
                coilErrorExtensionStub,
                kotlin(
                    """
                    package test

                    import android.widget.ImageView
                    import coil3.load
                    import coil3.request.error

                    fun test(imageView: ImageView) {
                        imageView.load("https://example.com/image.jpg") {
                            error(android.R.drawable.ic_delete) // Correct: Coil's error()
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testStdlibErrorOutsideCoilContextDoesNotTriggerWarning() {
        lint()
            .allowMissingSdk()
            .files(
                kotlinErrorStub,
                kotlin(
                    """
                    package test

                    import kotlin.error

                    fun validateInput(input: String) {
                        if (input.isEmpty()) {
                            error("Input cannot be empty") // This is fine - not in Coil context
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testStdlibErrorInRegularLambdaDoesNotTriggerWarning() {
        lint()
            .allowMissingSdk()
            .files(
                kotlinErrorStub,
                kotlin(
                    """
                    package test

                    import kotlin.error

                    fun process(items: List<String>) {
                        items.forEach { item ->
                            if (item.isEmpty()) {
                                error("Empty item found") // This is fine - not in Coil context
                            }
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }
}
