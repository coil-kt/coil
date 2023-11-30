package coil

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

fun Project.addAllMultiplatformTargets() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.getByType<KotlinMultiplatformExtension>().apply {
            val isAndroidApp = plugins.hasPlugin("com.android.application")
            val isAndroidLibrary = plugins.hasPlugin("com.android.library")
            if (isAndroidApp || isAndroidLibrary) {
                androidTarget {
                    if (isAndroidLibrary) {
                        publishLibraryVariants("release")
                    }
                }
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            js {
                browser()
                binaries.executable()
            }

            jvm()

            macosX64()
            macosArm64()

            applyDefaultHierarchyTemplate()
        }
    }
}

fun KotlinSourceSetContainer.jvmCommon() = sourceSet(
    name = "jvmCommon",
    children = listOf("androidMain", "jvmMain"),
)

fun KotlinSourceSetContainer.nonAndroidMain() = sourceSet(
    name = "nonAndroidMain",
    children = listOf("jsMain", "jvmMain", "nativeMain"),
)

fun KotlinSourceSetContainer.nonJsMain() = sourceSet(
    name = "nonJsMain",
    children = listOf("jvmCommon", "nativeMain"),
)

fun KotlinSourceSetContainer.nonJvmCommon() = sourceSet(
    name = "nonJvmCommon",
    children = listOf("jsMain", "nativeMain"),
)

fun KotlinSourceSetContainer.sourceSet(
    name: String,
    children: List<String>,
) {
    val sourceSet = sourceSets.create(name)
    sourceSet.dependsOn(sourceSets.getByName("commonMain"))
    for (child in children) {
        sourceSets.getByName(child).dependsOn(sourceSet)
    }
}
