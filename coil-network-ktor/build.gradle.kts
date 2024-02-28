import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.androidUnitTest

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.network.ktor")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.ktor.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                implementation(libs.ktor.mock)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}

// https://youtrack.jetbrains.com/issue/KTOR-5587
repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
}
project.configurations.all {
    if (name.startsWith("wasmJs")) {
        resolutionStrategy.eachDependency {
            if (requested.group.startsWith("io.ktor") &&
                requested.name.startsWith("ktor-client-")) {
                useVersion("3.0.0-wasm2")
            }
        }
    }
}
