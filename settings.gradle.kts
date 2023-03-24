rootProject.name = "coil"

// https://docs.gradle.org/7.4/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Public modules
include(
    "coil-base",
    "coil-singleton",
    "coil-compose-base",
    "coil-compose-singleton",
    "coil-gif",
    "coil-svg",
    "coil-video",
    "coil-bom",
    "coil-test",
)

// Private modules
include(
    "coil-benchmark",
    "coil-sample-common",
    "coil-sample-compose",
    "coil-sample-view",
    "coil-test-internal",
    "coil-test-paparazzi",
)
