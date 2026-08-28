import coil3.addNodePolyfillWebpackPlugin
import coil3.createSkikoWasmJsRuntimeDependency

plugins {
    id("kotlin-multiplatform")
}

kotlin {
    js {
        useEsModules()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    useConfigDirectory(rootProject.projectDir.resolve("karma.config.d"))
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsTest {
            dependencies {
                implementation(projects.coilCore)
                implementation(libs.bundles.test.common)
            }
        }
    }
}

addNodePolyfillWebpackPlugin(enableWasm = false)
createSkikoWasmJsRuntimeDependency(libs.versions.skiko)
