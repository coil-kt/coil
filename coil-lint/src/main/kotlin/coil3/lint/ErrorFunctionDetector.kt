package coil3.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression

class ErrorFunctionDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("error")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        // Check if this is kotlin.error() from stdlib
        if (!isKotlinStdlibError(method)) {
            return
        }

        // Check if we're inside an ImageRequest.Builder lambda context
        if (!isInsideImageRequestBuilderLambda(node)) {
            return
        }

        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Using `kotlin.error()` inside `ImageRequest.Builder`. " +
                "Did you mean to use Coil's `error()` extension function to set an error drawable? " +
                "Consider importing `coil3.request.error` instead.",
        )
    }

    private fun isKotlinStdlibError(method: PsiMethod): Boolean {
        val containingClass = method.containingClass?.qualifiedName ?: return false

        // Standard stdlib class names
        if (containingClass == "kotlin.PreconditionsKt" ||
            containingClass == "kotlin.PreconditionsKt__PreconditionsKt"
        ) {
            return true
        }

        // For test stubs and some Kotlin versions, the file facade might have different names
        // Check if it's a top-level function in the kotlin package that returns Nothing
        if (containingClass.startsWith("kotlin.") &&
            method.returnType?.canonicalText == "kotlin.Nothing"
        ) {
            return true
        }

        return false
    }

    private fun isInsideImageRequestBuilderLambda(node: UElement): Boolean {
        var current: UElement? = node
        while (current != null) {
            if (current is ULambdaExpression) {
                val lambdaParent = current.uastParent
                if (lambdaParent is UCallExpression) {
                    val receiverType = lambdaParent.receiverType?.canonicalText
                    val methodName = lambdaParent.methodName

                    // Check for ImageView.load { } or AsyncImage calls
                    if (methodName == "load" || methodName == "AsyncImage" || methodName == "SubcomposeAsyncImage") {
                        return true
                    }

                    // Check for ImageRequest.Builder { } or ImageRequest.Builder().apply { }
                    if (receiverType?.contains("ImageRequest.Builder") == true) {
                        return true
                    }
                    if (receiverType?.contains("ImageRequest\$Builder") == true) {
                        return true
                    }

                    // Check if the lambda is the trailing lambda of a function that returns/uses ImageRequest.Builder
                    val resolvedMethod = lambdaParent.resolve()
                    if (resolvedMethod != null) {
                        val returnType = resolvedMethod.returnType?.canonicalText ?: ""
                        if (returnType.contains("ImageRequest.Builder") || returnType.contains("ImageRequest\$Builder")) {
                            return true
                        }

                        // Check parameter types for builder lambda
                        for (param in resolvedMethod.parameterList.parameters) {
                            val paramType = param.type.canonicalText
                            if (paramType.contains("ImageRequest.Builder") || paramType.contains("ImageRequest\$Builder")) {
                                return true
                            }
                        }
                    }
                }
            }
            current = current.uastParent
        }
        return false
    }

    companion object {
        val ISSUE = Issue.create(
            id = "CoilErrorFunction",
            briefDescription = "Kotlin stdlib `error()` used inside ImageRequest.Builder",
            explanation = """
                Using Kotlin's stdlib `error()` function inside an `ImageRequest.Builder` lambda \
                (such as in `imageView.load { }` or `AsyncImage`) will throw an `IllegalStateException` \
                at runtime instead of setting an error drawable.

                This is likely a mistake caused by IDE auto-import selecting the wrong function. \
                Use Coil's `error(drawable)` extension function instead to set an error placeholder.

                **Wrong:**
                ```kotlin
                imageView.load(url) {
                    error(R.drawable.error) // This is kotlin.error() - throws exception!
                }
                ```

                **Correct:**
                ```kotlin
                import coil3.request.error

                imageView.load(url) {
                    error(R.drawable.error) // This is coil3.request.error() - sets error drawable
                }
                ```
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                ErrorFunctionDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
