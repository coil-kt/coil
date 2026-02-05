import coil3.addAllMultiplatformTargets
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
    id("androidx.baselineprofile.consumer")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary(name = "coil3.core") {
    androidResources {
        enable = true
    }
    optimization {
        consumerKeepRules.publish = true
        consumerKeepRules.files += project.file("shrinker-rules.pro")
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                api(libs.skiko)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appcompat.resources)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
                implementation(libs.androidx.profileinstaller)
                implementation(libs.coroutines.android)
                api(libs.androidx.lifecycle.runtime)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
    }
}

baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    baselineProfileOutputDir = "."
    filter {
        include("coil3.**")
        exclude("coil3.compose.**")
        exclude("coil3.gif.**")
        exclude("coil3.network.**")
        exclude("coil3.svg.**")
        exclude("coil3.video.**")
    }
    variants {
        create("androidMain") {
            from(project(":internal:benchmark"))
        }
    }
}
