package coil

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

fun Project.setupLibraryModule(
    name: String,
    config: Boolean = false,
    action: LibraryExtension.() -> Unit = {},
) = setupBaseModule<LibraryExtension>(name) {
    buildFeatures {
        buildConfig = config
    }
    if (project.name in publicModules) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "com.vanniktech.maven.publish.base")
        setupPublishing {
            configure(AndroidSingleVariantLibrary())
        }
    }
    action()
}

fun Project.setupPublishing(
    action: MavenPublishBaseExtension.() -> Unit = {},
) {
    extensions.configure<MavenPublishBaseExtension> {
        pomFromGradleProperties()
        publishToMavenCentral()
        signAllPublications()
        action()

        coordinates(
            groupId = project.property("POM_GROUP_ID").toString(),
            artifactId = project.property("POM_ARTIFACT_ID").toString(),
            version = project.property("POM_VERSION").toString(),
        )
    }
}

fun Project.setupAppModule(
    name: String,
    action: BaseAppModuleExtension.() -> Unit = {},
) = setupBaseModule<BaseAppModuleExtension>(name) {
    defaultConfig {
        applicationId = name
        versionCode = project.versionCode
        versionName = project.versionName
        resourceConfigurations += "en"
        vectorDrawables.useSupportLibrary = true
    }
    action()
}

fun Project.setupTestModule(
    name: String,
    config: Boolean = false,
    action: TestExtension.() -> Unit = {},
) = setupBaseModule<TestExtension>(name) {
    buildFeatures {
        buildConfig = config
    }
    defaultConfig {
        resourceConfigurations += "en"
        vectorDrawables.useSupportLibrary = true
    }
    action()
}

private fun <T : BaseExtension> Project.setupBaseModule(
    name: String,
    action: T.() -> Unit,
) {
    android<T> {
        namespace = name
        compileSdkVersion(project.compileSdk)
        defaultConfig {
            minSdk = project.minSdk
            targetSdk = project.targetSdk
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        packagingOptions {
            resources.pickFirsts += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*kotlin_module",
            )
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
        lint {
            warningsAsErrors = true
            disable += listOf(
                "UnusedResources",
                "VectorPath",
                "VectorRaster",
            )
        }
        action()
    }
    kotlin {
        compilerOptions {
            allWarningsAsErrors by System.getenv("CI").toBoolean()

            val arguments = mutableListOf(
                // https://kotlinlang.org/docs/compiler-reference.html#progressive
                "-progressive",
                // Enable Java default method generation.
                "-Xjvm-default=all",
                // Generate smaller bytecode by not generating runtime not-null assertions.
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions",
            )
            if (project.name != "coil-benchmark" && project.name != "coil-test-internal") {
                arguments += "-opt-in=coil.annotation.ExperimentalCoilApi"
            }
            freeCompilerArgs.addAll(arguments)
        }
    }
}

private fun <T : BaseExtension> Project.android(action: T.() -> Unit) {
    extensions.configure("android", action)
}

private fun Project.kotlin(action: KotlinJvmCompile.() -> Unit) {
    tasks.withType<KotlinJvmCompile>().configureEach(action)
}

private fun BaseExtension.lint(action: Lint.() -> Unit) {
    (this as CommonExtension<*, *, *, *, *>).lint(action)
}
