package coil3

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

fun Project.addAllMultiplatformTargets() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.getByType<KotlinMultiplatformExtension>().apply {
            applyDefaultHierarchyTemplate()

            val isAndroidApp = plugins.hasPlugin("com.android.application")
            val isAndroidLibrary = plugins.hasPlugin("com.android.library")
            if (isAndroidApp || isAndroidLibrary) {
                androidTarget {
                    if (isAndroidLibrary) {
                        publishLibraryVariants("release")
                    }
                }
            }

            jvm()

            js {
                browser()
                nodejs {
                    testTask {
                        useMocha {
                            timeout = "60s"
                        }
                    }
                }
                binaries.executable()
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            macosX64()
            macosArm64()
        }

        applyOkioJsTestWorkaround()
    }
}

val NamedDomainObjectContainer<KotlinSourceSet>.androidUnitTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named("androidUnitTest")

val NamedDomainObjectContainer<KotlinSourceSet>.androidInstrumentedTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named("androidInstrumentedTest")

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

fun KotlinSourceSetContainer.nonJsTest() = sourceSet(
    name = "nonJsTest",
    children = listOf("androidUnitTest", "jvmTest", "nativeTest"),
    isTest = true,
)

fun KotlinSourceSetContainer.nonJvmCommon() = sourceSet(
    name = "nonJvmCommon",
    children = listOf("jsMain", "nativeMain"),
)

fun KotlinSourceSetContainer.sourceSet(
    name: String,
    children: List<String>,
    isTest: Boolean = false,
) {
    val sourceSet = sourceSets.create(name)
    val commonName = if (isTest) "commonTest" else "commonMain"
    sourceSet.dependsOn(sourceSets.getByName(commonName))
    for (child in children) {
        sourceSets.getByName(child).dependsOn(sourceSet)
    }
}

// https://github.com/square/okio/issues/1163
fun Project.applyOkioJsTestWorkaround() {
    val webpackConfigDir = projectDir.resolve("webpack.config.d").apply { mkdirs() }
    val applyPluginFile = webpackConfigDir.resolve("applyNodePolyfillPlugin.js")
    applyPluginFile.writeText(
        """
        const NodePolyfillPlugin = require("node-polyfill-webpack-plugin");
        config.plugins.push(new NodePolyfillPlugin())
        """.trimIndent(),
    )
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            configureEach {
                if (name == "jsTest") {
                    dependencies {
                        implementation(devNpm("node-polyfill-webpack-plugin", "^2.0.1"))
                    }
                }
            }
        }
    }
}
