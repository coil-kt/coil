pluginManagement {
    // Settings plugins are resolved before version catalog accessors are available.
    val versions = file("gradle/libs.versions.toml").readLines()
    fun version(name: String) = versions.first { it.startsWith("$name = ") }
        .substringAfter('"').substringBefore('"')

    plugins {
        id("com.android.settings") version version("android-gradle-plugin")
    }
    settings.extra["androidNdkVersion"] = version("android-ndk")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.android.settings")
}

android {
    ndkVersion = settings.extra["androidNdkVersion"] as String
}

rootProject.name = "coil-root"

// https://docs.gradle.org/7.4/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Public modules
include(
    "coil",
    "coil-core",
    "coil-compose",
    "coil-compose-core",
    "coil-network-core",
    "coil-network-ktor2",
    "coil-network-ktor3",
    "coil-network-okhttp",
    "coil-network-cache-control",
    "coil-gif",
    "coil-lint",
    "coil-svg",
    "coil-video",
    "coil-bom",
    "coil-test",
    "coil-version-catalog",
)

// Private modules
include(
    "internal:benchmark",
    "internal:test-compose-screenshot",
    "internal:test-compose-ui-multiplatform",
    "internal:test-js-es-modules",
    "internal:test-paparazzi",
    "internal:test-roborazzi",
    "internal:test-utils",
    "samples:compose",
    "samples:compose-android",
    "samples:shared",
    "samples:view",
)
