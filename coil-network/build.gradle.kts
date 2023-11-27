import coil.addAllMultiplatformTargets
import coil.nonAndroidMain
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil.network")

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilBase)
                api(libs.ktor.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.common)
                implementation(libs.ktor.mock)
            }
        }
    }
}
