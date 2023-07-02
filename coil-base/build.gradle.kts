import coil.addAllTargets
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllTargets(project)
setupLibraryModule(name = "coil.base")

kotlin {
    // nonAndroidMain: jsMain, jvmMain, nativeMain
    val nonAndroidMain = sourceSets.create("nonAndroidMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("jsMain").apply {
        dependsOn(nonAndroidMain)
    }
    sourceSets.getByName("jvmMain").apply {
        dependsOn(nonAndroidMain)
    }
    sourceSets.getByName("nativeMain").apply {
        dependsOn(nonAndroidMain)
    }

    // jvmCommon: androidMain, jvmMain
    val jvmCommon = sourceSets.create("jvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("androidMain").apply {
        dependsOn(jvmCommon)
    }
    sourceSets.getByName("jvmMain").apply {
        dependsOn(jvmCommon)
    }

    // nonJvmCommon: jsMain, nativeMain
    val nonJvmCommon = sourceSets.create("nonJvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("jsMain").apply {
        dependsOn(nonJvmCommon)
    }
    sourceSets.getByName("nativeMain").apply {
        dependsOn(nonJvmCommon)
    }

    // nonJsMain: androidMain, jvmMain, nativeMain
    val nonJsMain = sourceSets.create("nonJsMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("androidMain").apply {
        dependsOn(nonJsMain)
    }
    sourceSets.getByName("jvmMain").apply {
        dependsOn(nonJsMain)
    }
    sourceSets.getByName("nativeMain").apply {
        dependsOn(nonJsMain)
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test.common)
            }
        }
        named("jsMain") {
            dependencies {
                implementation(libs.okio.nodefilesystem)
            }
        }
        named("nonJvmCommon") {
            dependencies {
                implementation(libs.kotlinx.immutable.collections)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appcompat.resources)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
                implementation(libs.androidx.profileinstaller)
                api(libs.androidx.lifecycle.runtime)
                api(libs.coroutines.android)
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
