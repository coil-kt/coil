pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
    "coil-svg",
    "coil-video",
    "coil-bom",
    "coil-test",
)

// Private modules
include(
    "internal:benchmark",
    "internal:test-compose-screenshot",
    "internal:test-compose-ui-multiplatform",
    "internal:test-paparazzi",
    "internal:test-roborazzi",
    "internal:test-utils",
    "samples:compose",
    "samples:shared",
    "samples:view",
)
