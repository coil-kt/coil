import coil.Library
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule {
    defaultConfig {
        minSdk = 21
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Library.COMPOSE_VERSION
    }
}

dependencies {
    api(project(":coil-compose-base"))
    api(project(":coil-singleton"))
}
