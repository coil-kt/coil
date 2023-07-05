import coil.addAllTargets
import coil.createNonAndroidMain
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllTargets(project)
setupLibraryModule(name = "coil.singleton")

kotlin {
    createNonAndroidMain()

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
