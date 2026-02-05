import coil3.enableComposeMetrics
import coil3.groupId
import coil3.publicModules
import coil3.versionName
import com.diffplug.gradle.spotless.SpotlessExtension
import dev.drewhamilton.poko.gradle.PokoPluginExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
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
    alias(libs.plugins.spotless)
    // https://github.com/gradle/gradle/issues/20084#issuecomment-1060822638
    id("org.jetbrains.dokka")
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
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
        options.compilerArgs = options.compilerArgs + "-Xlint:-options"
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget = JvmTarget.JVM_11
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_2
        }
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

    extensions.configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt", "**/*.kts")
            ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
            endWithNewline()
            leadingTabsToSpaces()
            trimTrailingWhitespace()
        }
    }

    tasks.configureEach {
        if (name.startsWith("spotless")) {
            notCompatibleWithConfigurationCache("https://github.com/diffplug/spotless/issues/2459")
        }
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

    if (project.name in publicModules) {
        @OptIn(ExperimentalAbiValidation::class)
        plugins.withType<KotlinBasePlugin> {
            extensions.configure<KotlinProjectExtension> {
                fun AbiValidationVariantSpec.configure() = filters {
                    excluded {
                        annotatedWith.add("coil3.annotation.InternalCoilApi")
                    }
                }

                // Unfortunately the 'enabled' property doesn't share a common interface.
                val singleTargetExtension = extensions.findByType<AbiValidationExtension>()
                if (singleTargetExtension != null) {
                    singleTargetExtension.apply {
                        enabled = true
                        configure()
                    }
                } else {
                    extensions.configure<AbiValidationMultiplatformExtension> {
                        enabled = true
                        configure()
                    }
                }
            }
        }
    }

    // Skiko's runtime files are ESM-only so preload our shim to let Node require them during tests.
    tasks.withType<KotlinJsTest>().configureEach {
        nodeJsArgs.add("--require")
        val skikoMjsWorkaround = rootProject.layout.projectDirectory
            .file("gradle/nodejs/registerSkikoMjsWorkaround.cjs")
            .asFile
        nodeJsArgs.add(skikoMjsWorkaround.absolutePath)
    }
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

apply(from = rootProject.file("gradle/verifySkikoVersionsMatch.gradle.kts"))
