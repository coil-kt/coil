package coil

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

fun addAllTargets(project: Project) {
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
        project.extensions.getByType<KotlinMultiplatformExtension>().apply {
            val hasAndroidPlugin = project.plugins.hasPlugin("com.android.library")
            if (hasAndroidPlugin) {
                androidTarget {
                    publishLibraryVariants("release")
                }
            }

            iosArm64()
            iosX64()
            iosSimulatorArm64()

            js {
                browser()
                binaries.executable()
            }

            jvm()

            linuxX64()

            macosArm64()
            macosX64()

            applyDefaultHierarchyTemplate()
        }
    }
}

// nonAndroidMain: jsMain, jvmMain, nativeMain
fun KotlinSourceSetContainer.nonAndroidMain(): KotlinSourceSet {
    val nonAndroidMain = sourceSets.create("nonAndroidMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("jsMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonAndroidMain)
    }
    return nonAndroidMain
}

// nonJsMain: androidMain, jvmMain, nativeMain
fun KotlinSourceSetContainer.nonJsMain(): KotlinSourceSet {
    val nonJsMain = sourceSets.create("nonJsMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("androidMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonJsMain)
    }
    return nonJsMain
}

// jvmCommon: androidMain, jvmMain
fun KotlinSourceSetContainer.jvmCommon(): KotlinSourceSet {
    val jvmCommon = sourceSets.create("jvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("androidMain", "jvmMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(jvmCommon)
    }
    return jvmCommon
}

// nonJvmCommon: jsMain, nativeMain
fun KotlinSourceSetContainer.nonJvmCommon(): KotlinSourceSet {
    val nonJvmCommon = sourceSets.create("nonJvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("jsMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonJvmCommon)
    }
    return nonJvmCommon
}
