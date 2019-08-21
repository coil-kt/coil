import coil.Library
import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.compileSdk
import coil.minSdk
import coil.targetSdk
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("kotlin-android")
    id("org.jetbrains.dokka-android")
}

android {
    compileSdkVersion(project.compileSdk)
    defaultConfig {
        minSdkVersion(project.minSdk)
        targetSdkVersion(project.targetSdk)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = false }
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

afterEvaluate {
    tasks.withType<DokkaAndroidTask> {
        outputDirectory = "$rootDir/docs/api"
        outputFormat = "gfm"
    }
}

dependencies {
    api(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    api(Library.KOTLINX_COROUTINES_ANDROID)

    implementation(Library.ANDROIDX_ANNOTATION)
    implementation(Library.ANDROIDX_COLLECTION)
    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_EXIF_INTERFACE)

    api(Library.ANDROIDX_LIFECYCLE_COMMON)

    // Optional: only use this dependency if it is present in the classpath at runtime.
    compileOnly(Library.ANDROIDX_APPCOMPAT)

    api(Library.OKHTTP)
    api(Library.OKIO)

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
