import coil.Library
import coil.setupLibraryModule

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
}
