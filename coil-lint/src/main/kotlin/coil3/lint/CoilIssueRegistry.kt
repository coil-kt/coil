package coil3.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class CoilIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        ErrorFunctionDetector.ISSUE,
    )

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "Coil",
        identifier = "coil",
        feedbackUrl = "https://github.com/coil-kt/coil/issues",
    )
}
