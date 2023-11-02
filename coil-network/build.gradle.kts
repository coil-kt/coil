import coil.addAllTargets
import coil.jvmCommon
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllTargets(project)
setupLibraryModule(name = "coil.network")

kotlin {
    jvmCommon()

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
