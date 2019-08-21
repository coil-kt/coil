import coil.groupId
import coil.versionName
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URL

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://plugins.gradle.org/m2/")
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.9.0-SNAPSHOT")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:8.2.0")
        classpath(kotlin("gradle-plugin", version = "1.3.41"))
    }
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

    @Suppress("UnstableApiUsage")
    extensions.configure<KtlintExtension>("ktlint") {
        version.set("0.34.2")
        enableExperimentalRules.set(true)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    tasks.withType<Test> {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED)
            showStandardStreams = true
        }
    }

    // Must be afterEvaluate or else com.vanniktech.maven.publish will overwrite our dokka configuration.
    afterEvaluate {
        tasks.withType<DokkaAndroidTask> {
            jdkVersion = 8
            reportUndocumented = false
            skipDeprecated = true

            externalDocumentationLink {
                url = URL("https://developer.android.com/reference/androidx/")
            }
            externalDocumentationLink {
                url = URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-android/")
            }
            externalDocumentationLink {
                url = URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/")
            }
            externalDocumentationLink {
                url = URL("https://square.github.io/okhttp/3.x/okhttp/")
            }
            externalDocumentationLink {
                url = URL("https://square.github.io/okio/2.x/okio/")
            }
            linkMapping {
                dir = "src/main/java"
                url = "https://github.com/coil-kt/coil"
                suffix = "#L"
            }
        }
    }
}
