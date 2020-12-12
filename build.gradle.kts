import coil.by
import coil.groupId
import coil.versionName
import com.android.build.gradle.BaseExtension
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URL

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.13.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.10.1")
        classpath("org.jetbrains.kotlinx:binary-compatibility-validator:0.2.4")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:9.4.1")
        classpath(kotlin("gradle-plugin", version = "1.4.21"))
    }
}

apply(plugin = "binary-compatibility-validator")

extensions.configure<ApiValidationExtension> {
    ignoredProjects = mutableSetOf("coil-sample", "coil-test")
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    group = project.groupId
    version = project.versionName

    extensions.configure<KtlintExtension>("ktlint") {
        version by "0.40.0"
        disabledRules by setOf("indent", "max-line-length")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            val arguments = mutableListOf("-progressive", "-Xopt-in=kotlin.RequiresOptIn")
            if (project.name != "coil-test") {
                arguments += "-Xopt-in=coil.annotation.ExperimentalCoilApi"
                arguments += "-Xopt-in=coil.annotation.InternalCoilApi"
            }
            freeCompilerArgs = arguments
            jvmTarget = "1.8"
        }
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED)
            showStandardStreams = true
        }
    }

    // Must be afterEvaluate or else com.vanniktech.maven.publish will overwrite our dokka configuration.
    afterEvaluate {
        tasks.withType<DokkaTask>().configureEach {
            configuration {
                jdkVersion = 8
                reportUndocumented = false
                skipDeprecated = true
                skipEmptyPackages = true
                outputDirectory = "$rootDir/docs/api"
                outputFormat = "gfm"

                externalDocumentationLink {
                    url = URL("https://developer.android.com/reference/")
                    packageListUrl = URL("https://developer.android.com/reference/androidx/package-list")
                }
                externalDocumentationLink {
                    url = URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-android/")
                    packageListUrl = URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-android/package-list")
                }
                externalDocumentationLink {
                    url = URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/")
                    packageListUrl = URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/package-list")
                }
                externalDocumentationLink {
                    url = URL("https://square.github.io/okhttp/3.x/okhttp/")
                    packageListUrl = URL("https://square.github.io/okhttp/3.x/okhttp/package-list")
                }
                externalDocumentationLink {
                    url = URL("https://square.github.io/okio/2.x/okio/")
                    packageListUrl = URL("file://$rootDir/package-list-okio")
                }

                // Include the coil-base documentation link for extension artifacts.
                if (project.name != "coil-base") {
                    externalDocumentationLink {
                        url = URL("https://coil-kt.github.io/coil/api/coil-base/")
                        packageListUrl = URL("file://$rootDir/package-list-coil-base")
                    }
                }
            }
        }
    }
}

subprojects {
    afterEvaluate {
        extensions.configure<BaseExtension> {
            // Require that all Android projects target Java 8.
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }
}
