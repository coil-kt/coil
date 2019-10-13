import coil.Library
import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.compileSdk
import coil.minSdk
import coil.targetSdk
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.net.URL

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

afterEvaluate {
    tasks.withType<DokkaAndroidTask> {
        outputDirectory = "$rootDir/docs/api"
        outputFormat = "gfm"

        configuration {
            externalDocumentationLink {
                url = URL("file://$rootDir/docs/api/coil-base/")
                packageListUrl = URL("file://$rootDir/docs/api/coil-base/package-list")
            }
        }
    }
}

dependencies {
    api(project(":coil-base"))

    implementation(Library.ANDROIDX_CORE)

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
