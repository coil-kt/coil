import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.setupLibraryModule
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("kotlin-android")
}

setupLibraryModule()

dependencies {
    api(project(":coil-base"))

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
