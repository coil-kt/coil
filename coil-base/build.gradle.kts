import coil.addAllTargets
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

setupLibraryModule(name = "coil.base")
addAllTargets(project)

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio)
            }
        }
        named("jsNativeMain") {
            dependencies {
                implementation(libs.kotlinx.immutable.collections)
            }
        }
        android {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appcompat.resources)
                implementation(libs.androidx.collection)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
                implementation(libs.androidx.profileinstaller)
                api(libs.androidx.lifecycle.runtime)
                api(libs.coroutines.android)
                api(libs.okhttp)

                testImplementation(projects.coilTestInternal)
                testImplementation(libs.bundles.test.jvm)

                androidTestImplementation(projects.coilTestInternal)
                androidTestImplementation(libs.bundles.test.android)
            }
        }
    }
}
