import coil.addAllTargets
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllTargets(project)
setupLibraryModule(name = "coil.singleton")

kotlin {
    // nonAndroidMain: jsMain, jvmMain, nativeMain
    val nonAndroidMain = sourceSets.create("nonAndroidMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("jsMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonAndroidMain)
    }

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
