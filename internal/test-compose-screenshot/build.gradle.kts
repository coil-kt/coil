import coil3.androidLibrary
import com.android.build.api.dsl.AgpTestSuiteInputParameters
import com.android.build.gradle.tasks.TestSuiteTestTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

androidLibrary(name = "coil3.test.composescreenshot") {
    buildFeatures {
        compose = true
    }
    testOptions {
        screenshotTests.create("screenshotTest") {
            engineVersion = libs.screenshot.validation.api.get().versionConstraint.requiredVersion
            imageDifferenceThreshold = 0.01f
            targetVariants.add("debug")

            dependencies {
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.screenshot.validation.api)
            }
        }
    }
}

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)
}

// AGP 9.5.0-alpha09 supplies generated R classes separately from the renderer's classpath.
tasks.withType<TestSuiteTestTask>().matching {
    it.name.endsWith("ScreenshotTestDefaultTestSuite")
}.configureEach {
    val rendererClasspath = objects.fileCollection().from(
        engineInputParameters.map { parameters ->
            parameters.filter {
                it.type == AgpTestSuiteInputParameters.TEST_CLASSPATH ||
                    it.type == AgpTestSuiteInputParameters.R_CLASS_JARS
            }.map { it.fileCollection }
        },
    )
    jvmArgumentProviders.add(ScreenshotClasspathArgumentProvider(rendererClasspath))
}

private class ScreenshotClasspathArgumentProvider(
    @get:Classpath val classpath: FileCollection,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        return listOf(
            "-DPreviewScreenshotTestEngineInput.Renderer.screenshotAllClassPath=${classpath.asPath}",
        )
    }
}
