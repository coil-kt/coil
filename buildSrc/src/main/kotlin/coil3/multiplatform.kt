package coil3

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

fun Project.addAllMultiplatformTargets(
    skikoVersion: Provider<String>,
    enableWasm: Boolean = true,
    enableNativeLinux: Boolean = true,
) {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            applyCoilHierarchyTemplate()

            jvm()

            js {
                browser {
                    testTask {
                        useKarma { useChromeHeadless() }
                    }
                }
                nodejs {
                    testTask {
                        enabled = false
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
                    val skikoDirProvider = rootProject.layout
                        .buildDirectory
                        .dir("wasm/packages/coil-root${path.replace(":", "-")}-test/kotlin")
                        .map { it.asFile }

                    browser {
                        testTask {
                            useKarma { useChromeHeadless() }
                        }
                    }
                    nodejs {
                        testTask {
                            enabled = false
                            doFirst {
                                val skikoModule = skikoDirProvider.get().resolve("skiko.mjs")
                                if (skikoModule.isFile) {
                                    // The generated Skiko module disables its Node-specific loader
                                    // with `if (false)`. Rewrite it so the Node path runs and the
                                    // tests can load `skiko.wasm`.
                                    val original = skikoModule.readText()
                                    val patched = original.replaceFirst(
                                        "if (false) {",
                                        "if (ENVIRONMENT_IS_NODE) {"
                                    )
                                    if (patched != original) {
                                        skikoModule.writeText(patched)
                                    }
                                }
                            }
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

        addNodePolyfillWebpackPlugin(enableWasm)
        applyKotlinJsImplicitDependencyWorkaround(enableWasm)
        createSkikoWasmJsRuntimeDependency(skikoVersion)
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

// https://github.com/square/okio/issues/1163
fun Project.addNodePolyfillWebpackPlugin(
    enableWasm: Boolean = true,
) {
    extensions.configure<KotlinMultiplatformExtension> {
        val nodePolyfillPlugin = NpmDependency(
            project.objects,
            NpmDependency.Scope.DEV,
            "node-polyfill-webpack-plugin",
            "^2.0.1",
        )
        sourceSets {
            getByName("jsMain").dependencies {
                implementation(nodePolyfillPlugin)
            }
            if (enableWasm) {
                getByName("wasmJsMain").dependencies {
                    implementation(nodePolyfillPlugin)
                }
            }
        }
    }
}
