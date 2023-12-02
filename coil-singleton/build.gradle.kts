import coil3.addAllMultiplatformTargets
import coil3.nonAndroidMain
import coil3.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil3.singleton")

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilBase)
            }
        }
        named("androidUnitTest") {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.jvm)
            }
        }
        named("androidInstrumentedTest") {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
