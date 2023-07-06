import coil.addAllTargets
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

addAllTargets(project)
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
