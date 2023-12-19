import coil3.enableComposeMetrics
import coil3.groupId
import coil3.publicModules
import coil3.versionName
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import dev.drewhamilton.poko.gradle.PokoPluginExtension
import java.net.URL
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    dependencies {
        classpath(libs.gradlePlugin.android)
        classpath(libs.gradlePlugin.atomicFu)
        classpath(libs.gradlePlugin.jetbrainsCompose)
        classpath(libs.gradlePlugin.kotlin)
        classpath(libs.gradlePlugin.mavenPublish)
        classpath(libs.gradlePlugin.paparazzi)
        classpath(libs.gradlePlugin.roborazzi)
    }
}

plugins {
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.dokka)
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.spotless)
}

extensions.configure<ApiValidationExtension> {
    ignoredProjects += project.subprojects
        .mapNotNull { if (it.name in publicModules) null else it.name }
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory = layout.projectDirectory.dir("docs/api")
}

plugins.withType<NodeJsRootPlugin> {
    extensions.getByType<NodeJsRootExtension>().apply {
        // WASM requires a canary Node.js version.
        nodeVersion = "21.0.0-v8-canary20231024d0ddc81258"
        nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
    }
}

tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }

    // Necessary to publish to Maven.
    group = groupId
    version = versionName

    // Target JVM 8.
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.compilerArgs = options.compilerArgs + "-Xlint:-options"
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion = 8
            failOnWarning = true
            skipDeprecated = true
            suppressInheritedMembers = true

            externalDocumentationLink {
                url = URL("https://developer.android.com/reference/")
            }
            externalDocumentationLink {
                url = URL("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
            externalDocumentationLink {
                url = URL("https://square.github.io/okio/3.x/okio/")
                packageListUrl = URL("https://square.github.io/okio/3.x/okio/okio/package-list")
            }
        }
    }

    dependencies {
        modules {
            module("org.jetbrains.kotlin:kotlin-stdlib-jdk7") {
                replacedBy("org.jetbrains.kotlin:kotlin-stdlib")
            }
            module("org.jetbrains.kotlin:kotlin-stdlib-jdk8") {
                replacedBy("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
    }

    // Uninstall test APKs after running instrumentation tests.
    tasks.configureEach {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }

    apply(plugin = "com.diffplug.spotless")

    val configureSpotless: SpotlessExtension.() -> Unit = {
        kotlin {
            target("**/*.kt", "**/*.kts")
            ktlint(libs.versions.ktlint.get())
            endWithNewline()
            indentWithSpaces()
            trimTrailingWhitespace()
        }
    }

    if (project === rootProject) {
        spotless { predeclareDeps() }
        extensions.configure<SpotlessExtensionPredeclare>(configureSpotless)
    } else {
        extensions.configure(configureSpotless)
    }

    plugins.withId("dev.drewhamilton.poko") {
        extensions.configure<PokoPluginExtension> {
            pokoAnnotation = "coil3.annotation.Data"
        }
    }

    if (enableComposeMetrics && name in publicModules) {
        plugins.withId("org.jetbrains.compose") {
            tasks.withType<KotlinCompile> {
                val plugin = "plugin:androidx.compose.compiler.plugins.kotlin"
                val outputDir = layout.buildDirectory.dir("composeMetrics").get().asFile.path
                compilerOptions.freeCompilerArgs.addAll(
                    "-P", "$plugin:metricsDestination=$outputDir",
                    "-P", "$plugin:reportsDestination=$outputDir",
                )
            }
        }
    }

    applyOkioJsTestWorkaround()
}

// https://github.com/square/okio/issues/1163
fun Project.applyOkioJsTestWorkaround() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val applyNodePolyfillPlugin by lazy {
            tasks.register("applyNodePolyfillPlugin") {
                val applyPluginFile = projectDir
                    .resolve("webpack.config.d/applyNodePolyfillPlugin.js")
                onlyIf {
                    !applyPluginFile.exists()
                }
                doLast {
                    applyPluginFile.parentFile.mkdirs()
                    applyPluginFile.writeText(
                        """
                        const NodePolyfillPlugin = require("node-polyfill-webpack-plugin");
                        config.plugins.push(new NodePolyfillPlugin());
                        """.trimIndent(),
                    )
                }
            }
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                targets.configureEach {
                    compilations.configureEach {
                        if (platformType == KotlinPlatformType.js && name == "test") {
                            tasks
                                .getByName(compileKotlinTaskName)
                                .dependsOn(applyNodePolyfillPlugin)
                            dependencies {
                                implementation(devNpm("node-polyfill-webpack-plugin", "^2.0.1"))
                            }
                        }
                    }
                }
            }
        }
    }
}
