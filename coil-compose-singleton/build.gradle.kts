import coil.Library
import coil.setupCompose
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule {
    setupCompose()
}

dependencies {
    api(project(":coil-singleton"))
    api(project(":coil-compose-base"))

    implementation(Library.ANDROIDX_CORE)

    api(Library.COMPOSE_FOUNDATION)
}
