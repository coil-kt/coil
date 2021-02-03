import coil.Library
import coil.addAndroidTestDependencies
import coil.setupLibraryModule
import org.jetbrains.kotlin.config.KotlinCompilerVersion


plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("kotlin-android")
    id("org.jetbrains.dokka")
}

setupLibraryModule()

dependencies {
    api(project(":coil-base"))

    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_VECTOR_DRAWABLE_ANIMATED)

    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
