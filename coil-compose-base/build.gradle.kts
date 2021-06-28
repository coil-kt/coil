import coil.Library
import coil.addAndroidTestDependencies
import coil.addTestDependencies
import coil.setupLibraryModule
import org.jetbrains.kotlin.config.KotlinCompilerVersion

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
    api(project(":coil-base"))

    implementation(Library.ANDROIDX_CORE)

    api(Library.COMPOSE_FOUNDATION)

    addTestDependencies(KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(KotlinCompilerVersion.VERSION)

    androidTestImplementation(Library.COMPOSE_UI_TEST_JUNIT)
    androidTestImplementation(Library.COMPOSE_UI_TEST_MANIFEST)
}
