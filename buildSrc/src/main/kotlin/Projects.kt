package coil

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
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
    buildConfig: Boolean = false,
    publish: Boolean = false,
    block: LibraryExtension.() -> Unit = {}
) = setupBaseModule<LibraryExtension> {
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = buildConfig }
    }
    if (publish) {
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
    block: BaseAppModuleExtension.() -> Unit = {}
) = setupBaseModule<BaseAppModuleExtension> {
    defaultConfig {
        versionCode = project.versionCode
        versionName = project.versionName
        resourceConfigurations += "en"
        vectorDrawables.useSupportLibrary = true
    }
    block()
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(
    crossinline block: T.() -> Unit = {}
) = extensions.configure<T>("android") {
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
            // Generate native Java 8 default interface methods.
            "-Xjvm-default=all",
            // Generate smaller bytecode by not generating runtime not-null assertions.
            "-Xno-call-assertions",
            "-Xno-param-assertions",
            "-Xno-receiver-assertions",
            // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-requires-opt-in/#requiresoptin
            "-Xopt-in=kotlin.RequiresOptIn"
        )
        if (project.name != "coil-test") {
            arguments += "-Xopt-in=coil.annotation.ExperimentalCoilApi"
        }
        // https://youtrack.jetbrains.com/issue/KT-41985
        freeCompilerArgs += arguments
    }
    packagingOptions {
        resources.pickFirsts += "META-INF/AL2.0"
        resources.pickFirsts += "META-INF/LGPL2.1"
        resources.pickFirsts += "META-INF/*kotlin_module"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    block()
}

private fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}
