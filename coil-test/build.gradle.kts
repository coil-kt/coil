import coil.addAllMultiplatformTargets
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil.test")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilBase)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test.common)
            }
        }
    }
}
