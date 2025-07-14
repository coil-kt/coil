package coil3

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.addAllMultiplatformTargets(
    skikoVersion: Provider<String>,
    enableWasm: Boolean = true,
    enableNativeLinux: Boolean = true,
) {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            applyCoilHierarchyTemplate()

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
                binaries.library()
            }

            if (enableWasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    // TODO: Fix wasm tests.
                    browser {
                        testTask {
                            enabled = false
                        }
                    }
                    nodejs {
                        testTask {
                            enabled = false
                        }
                    }
                    binaries.executable()
                    binaries.library()
                }
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            macosX64()
            macosArm64()

            if (enableNativeLinux) {
                linuxX64()
                linuxArm64()
            }
        }

        applyKotlinJsImplicitDependencyWorkaround(enableWasm)
        if (enableWasm) {
            createSkikoWasmJsRuntimeDependency(skikoVersion)
        }
    }
}

// https://youtrack.jetbrains.com/issue/KT-56025
fun Project.applyKotlinJsImplicitDependencyWorkaround(enableWasm: Boolean = true) {
    tasks {
        val configureJs: Task.() -> Unit = {
            dependsOn(named("jsDevelopmentLibraryCompileSync"))
            dependsOn(named("jsDevelopmentExecutableCompileSync"))
            dependsOn(named("jsProductionLibraryCompileSync"))
            dependsOn(named("jsProductionExecutableCompileSync"))
            dependsOn(named("jsTestTestDevelopmentExecutableCompileSync"))

            dependsOn(getByPath(":coil:jsDevelopmentLibraryCompileSync"))
            dependsOn(getByPath(":coil:jsDevelopmentExecutableCompileSync"))
            dependsOn(getByPath(":coil:jsProductionLibraryCompileSync"))
            dependsOn(getByPath(":coil:jsProductionExecutableCompileSync"))
            dependsOn(getByPath(":coil:jsTestTestDevelopmentExecutableCompileSync"))
        }
        named("jsBrowserProductionWebpack").configure(configureJs)
        named("jsBrowserProductionLibraryDistribution").configure(configureJs)
        named("jsNodeProductionLibraryDistribution").configure(configureJs)

        if (enableWasm) {
            val configureWasmJs: Task.() -> Unit = {
                dependsOn(named("wasmJsDevelopmentLibraryCompileSync"))
                dependsOn(named("wasmJsDevelopmentExecutableCompileSync"))
                dependsOn(named("wasmJsProductionLibraryCompileSync"))
                dependsOn(named("wasmJsProductionExecutableCompileSync"))
                dependsOn(named("wasmJsTestTestDevelopmentExecutableCompileSync"))

                dependsOn(getByPath(":coil:wasmJsDevelopmentLibraryCompileSync"))
                dependsOn(getByPath(":coil:wasmJsDevelopmentExecutableCompileSync"))
                dependsOn(getByPath(":coil:wasmJsProductionLibraryCompileSync"))
                dependsOn(getByPath(":coil:wasmJsProductionExecutableCompileSync"))
                dependsOn(getByPath(":coil:wasmJsTestTestDevelopmentExecutableCompileSync"))
            }
            named("wasmJsBrowserProductionWebpack").configure(configureWasmJs)
            named("wasmJsBrowserProductionLibraryDistribution").configure(configureWasmJs)
            named("wasmJsNodeProductionLibraryDistribution").configure(configureWasmJs)
        }
    }
}
