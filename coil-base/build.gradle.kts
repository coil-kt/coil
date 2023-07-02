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
    listOf("jsMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonAndroidMain)
    }

    // jvmCommon: androidMain, jvmMain
    val jvmCommon = sourceSets.create("jvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("androidMain", "jvmMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(jvmCommon)
    }

    // nonJvmCommon: jsMain, nativeMain
    val nonJvmCommon = sourceSets.create("nonJvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("jsMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonJvmCommon)
    }

    // nonJsMain: androidMain, jvmMain, nativeMain
    val nonJsMain = sourceSets.create("nonJsMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("androidMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonJsMain)
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.ktor.core)
                api(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test.common)
            }
        }
        named("nonJvmCommon") {
            dependencies {
                implementation(libs.kotlinx.immutable.collections)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                implementation(libs.skiko)
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
