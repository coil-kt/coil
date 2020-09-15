import coil.Library
import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.compileSdk
import coil.minSdk
import coil.targetSdk
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("kotlin-android")
    id("org.jetbrains.dokka")
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
    api(project(":coil-base"))

    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROID_SVG)

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
