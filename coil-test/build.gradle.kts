import coil.Library
import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.compileSdk
import coil.minSdk
import coil.targetSdk
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdkVersion(project.compileSdk)
    defaultConfig {
        minSdkVersion(project.minSdk)
        targetSdkVersion(project.targetSdk)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = false }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // compileOnly to avoid circular dependency issues.
    compileOnly(project(":coil-base"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation(Library.KOTLINX_COROUTINES_ANDROID)

    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_LIFECYCLE_COMMON)

    implementation(Library.OKHTTP)
    implementation(Library.OKHTTP_MOCK_WEB_SERVER)

    implementation(Library.OKIO)

    addTestDependencies(KotlinCompilerVersion.VERSION, includeTestProject = false)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION, includeTestProject = false)
}
