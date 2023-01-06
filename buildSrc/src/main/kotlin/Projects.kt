package coil

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

fun Project.setupLibraryModule(
    name: String?,
    buildConfig: Boolean = false,
    publish: Boolean = false,
    document: Boolean = publish,
    block: LibraryExtension.() -> Unit = {},
) = setupBaseModule<LibraryExtension>(name) {
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = buildConfig }
    }
    if (publish) {
        if (document) apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "com.vanniktech.maven.publish.base")
        publishing {
            singleVariant("release") {
                withJavadocJar()
                withSourcesJar()
            }
        }
        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications.create<MavenPublication>("release") {
                    from(components["release"])
                    // https://github.com/vanniktech/gradle-maven-publish-plugin/issues/326
                    val id = project.property("POM_ARTIFACT_ID").toString()
                    artifactId = artifactId.replace(project.name, id)
                }
            }
        }
    }
    block()
}

fun Project.setupAppModule(
    name: String?,
    block: BaseAppModuleExtension.() -> Unit = {}
) = setupBaseModule<BaseAppModuleExtension>(name) {
    defaultConfig {
        applicationId = name
        versionCode = project.versionCode
        versionName = project.versionName
        resourceConfigurations += "en"
        vectorDrawables.useSupportLibrary = true
    }
    block()
}

fun Project.setupTestModule(
    name: String?,
    block: TestExtension.() -> Unit = {},
) = setupBaseModule<TestExtension>(name) {
    defaultConfig {
        resourceConfigurations += "en"
        vectorDrawables.useSupportLibrary = true
    }
    block()
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(
    name: String?,
    crossinline block: T.() -> Unit = {},
) = extensions.configure<T>("android") {
    namespace = name
    compileSdkVersion(project.compileSdk)
    defaultConfig {
        minSdk = project.minSdk
        targetSdk = project.targetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = true

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
        // https://youtrack.jetbrains.com/issue/KT-41985
        freeCompilerArgs += arguments
    }
    packagingOptions {
        resources.pickFirsts += arrayOf(
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
        disable += arrayOf(
            "UnusedResources",
            "VectorPath",
            "VectorRaster",
        )
    }
    block()
}

private fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}

private fun BaseExtension.lint(block: Lint.() -> Unit) {
    (this as CommonExtension<*, *, *, *>).lint(block)
}
