package coil

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
    setupBaseModule<LibraryExtension> {
        libraryVariants.all {
            generateBuildConfigProvider?.configure { enabled = false }
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
        block()
    }
}

fun CommonExtension<*, *, *, *>.setupCompose() {
    defaultConfig.minSdk = 21
    buildFeatures.compose = true
    composeOptions {
        kotlinCompilerExtensionVersion = Library.COMPOSE_VERSION
    }
}

fun Project.setupAppModule(block: BaseAppModuleExtension.() -> Unit = {}) {
    setupBaseModule<BaseAppModuleExtension> {
        defaultConfig {
            versionCode = project.versionCode
            versionName = project.versionName
            resourceConfigurations += "en"
            vectorDrawables.useSupportLibrary = true
        }
        block()
    }
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
    extensions.configure<BaseExtension>("android") {
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
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            allWarningsAsErrors = true

            val arguments = mutableListOf("-progressive", "-Xopt-in=kotlin.RequiresOptIn")
            if (project.name != "coil-test") {
                arguments += "-Xopt-in=coil.annotation.ExperimentalCoilApi"
                arguments += "-Xopt-in=coil.annotation.InternalCoilApi"
            }
            freeCompilerArgs = arguments
        }
        packagingOptions {
            resources.pickFirsts += "META-INF/AL2.0"
            resources.pickFirsts += "META-INF/LGPL2.1"
            resources.pickFirsts += "META-INF/*kotlin_module"
        }
        (this as T).block()
    }
}

private fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}
