import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.androidUnitTest

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.test")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.coroutines.swing)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
