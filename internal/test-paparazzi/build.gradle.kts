import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
    id("org.jetbrains.compose")
}

androidLibrary(name = "coil3.test.paparazzi")

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)
}

// https://github.com/google/guava/issues/6801
dependencies.constraints {
    add("testImplementation", "com.google.guava:guava") {
        attributes {
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.STANDARD_JVM),
            )
        }
    }
}
