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
    id("kotlin-android-extensions")
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

androidExtensions {
    isExperimental = true
    features = setOf("parcelize")
}

dependencies {
    api(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    api(Library.KOTLINX_COROUTINES_ANDROID)

    implementation(Library.ANDROIDX_ANNOTATION)
    implementation(Library.ANDROIDX_APPCOMPAT_RESOURCES)
    implementation(Library.ANDROIDX_COLLECTION)
    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_EXIF_INTERFACE)

    api(Library.ANDROIDX_LIFECYCLE_COMMON)
    implementation(Library.ANDROIDX_LIFECYCLE_RUNTIME)

    api(Library.OKHTTP)
    api(Library.OKIO)

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
