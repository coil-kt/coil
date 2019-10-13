import coil.Library
import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.compileSdk
import coil.minSdk
import coil.targetSdk
import org.jetbrains.dokka.gradle.DokkaTask
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
    sourceSets {
        getByName("test").apply {
            assets.srcDirs("src/sharedTest/assets")
            java.srcDirs("src/sharedTest/java")
        }
        getByName("androidTest").apply {
            assets.srcDirs("src/sharedTest/assets")
            java.srcDirs("src/sharedTest/java")
        }
    }
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = false }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

afterEvaluate {
    tasks.withType<DokkaTask> {
        outputDirectory = "$rootDir/docs/api"
        outputFormat = "gfm"
    }
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

    api(Library.OKHTTP)
    api(Library.OKIO)

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
