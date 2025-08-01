import coil3.enableComposeMetrics
import coil3.groupId
import coil3.publicModules
import coil3.versionName
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import dev.drewhamilton.poko.gradle.PokoPluginExtension
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.wasm
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradlePlugin.android)
        classpath(libs.gradlePlugin.atomicFu)
        classpath(libs.gradlePlugin.jetbrainsCompose)
        classpath(libs.gradlePlugin.composeCompiler)
        classpath(libs.gradlePlugin.kotlin)
        classpath(libs.gradlePlugin.mavenPublish)
        classpath(libs.gradlePlugin.paparazzi)
        classpath(libs.gradlePlugin.roborazzi)
    }
}

plugins {
    alias(libs.plugins.baselineProfile) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.spotless)
    // https://github.com/gradle/gradle/issues/20084#issuecomment-1060822638
    id("org.jetbrains.dokka")
}

extensions.configure<ApiValidationExtension> {
    nonPublicMarkers += "coil3/annotation/InternalCoilApi"
    ignoredProjects += project.subprojects.mapNotNull { project ->
        if (project.name in publicModules) null else project.name
    }
    @OptIn(ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

dokka {
    dokkaGeneratorIsolation = ClassLoaderIsolation()
    dokkaPublications.configureEach {
        outputDirectory.set(layout.projectDirectory.dir("docs/api"))
    }
}

dependencies {
    for (module in publicModules) {
        dokka(project(":$module"))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
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

    // Uninstall test APKs after running instrumentation tests.
    tasks.configureEach {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }

    // https://issuetracker.google.com/issues/411739086?pli=1
    tasks.withType<AbstractTestTask>().configureEach {
        failOnNoDiscoveredTests = false
    }

    apply(plugin = "com.diffplug.spotless")

    val configureSpotless: SpotlessExtension.() -> Unit = {
        kotlin {
            target("**/*.kt", "**/*.kts")
            ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
            endWithNewline()
            leadingTabsToSpaces()
            trimTrailingWhitespace()
        }
    }

    if (project === rootProject) {
        spotless { predeclareDeps() }
        extensions.configure<SpotlessExtensionPredeclare>(configureSpotless)
    } else {
        extensions.configure(configureSpotless)
    }

    plugins.withId("org.jetbrains.kotlin.plugin.compose") {
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.add {
                rootDir.resolve("coil-core/compose_compiler_config.conf")
            }

            if (enableComposeMetrics && name in publicModules) {
                val outputDir = layout.buildDirectory.dir("composeMetrics").get().asFile
                metricsDestination = outputDir
                reportsDestination = outputDir
            }
        }
    }

    plugins.withId("dev.drewhamilton.poko") {
        extensions.configure<PokoPluginExtension> {
            pokoAnnotation = "coil3/annotation/Poko"
        }
    }

    // https://youtrack.jetbrains.com/issue/CMP-5831
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name == "atomicfu") {
                useVersion(libs.versions.atomicfu.get())
            }
        }
    }

    applyOkioJsTestWorkaround()
}

private val ktlintRules = buildMap {
    put("ktlint_code_style", "intellij_idea")

    put("ktlint_standard_annotation", "disabled")
    put("ktlint_standard_blank-line-before-declaration", "disabled")
    put("ktlint_standard_class-signature", "disabled")
    put("ktlint_standard_filename", "disabled")
    put("ktlint_standard_function-expression-body", "disabled")
    put("ktlint_standard_function-signature", "disabled")
    put("ktlint_standard_function-literal", "disabled")
    put("ktlint_standard_indent", "disabled")
    put("ktlint_standard_max-line-length", "disabled")
    put("ktlint_standard_no-blank-line-in-list", "disabled")
    put("ktlint_standard_no-empty-first-line-in-class-body", "disabled")
    put("ktlint_standard_spacing-between-declarations-with-annotations", "disabled")
    put("ktlint_standard_string-template-indent", "disabled")
    put("ktlint_standard_trailing-comma-on-call-site", "disabled")
    put("ktlint_standard_trailing-comma-on-declaration-site", "disabled")
    put("ktlint_standard_try-catch-finally-spacing", "disabled")

    put("ktlint_standard_backing-property-naming", "disabled")
    put("ktlint_standard_function-naming", "disabled")
    put("ktlint_standard_property-naming", "disabled")

    put("ktlint_standard_type-argument-comment", "disabled")
    put("ktlint_standard_type-parameter-comment", "disabled")
    put("ktlint_standard_value-argument-comment", "disabled")
    put("ktlint_standard_value-parameter-comment", "disabled")

    put("ktlint_standard_argument-list-wrapping", "disabled")
    put("ktlint_standard_binary-expression-wrapping", "disabled")
    put("ktlint_standard_condition-wrapping", "disabled")
    put("ktlint_standard_context-receiver-wrapping", "disabled")
    put("ktlint_standard_enum-wrapping", "disabled")
    put("ktlint_standard_if-else-wrapping", "disabled")
    put("ktlint_standard_multiline-expression-wrapping", "disabled")
    put("ktlint_standard_parameter-wrapping", "disabled")
    put("ktlint_standard_parameter-list-wrapping", "disabled")
    put("ktlint_standard_property-wrapping", "disabled")
    put("ktlint_standard_statement-wrapping", "disabled")
    put("ktlint_standard_wrapping", "disabled")
}

// https://github.com/square/okio/issues/1163
fun Project.applyOkioJsTestWorkaround() {
    if (":samples" in displayName) {
        // The polyfills cause issues with the samples.
        return
    }

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
                        if ((platformType == js || platformType == wasm) && name == "test") {
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
