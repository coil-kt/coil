import coil.by
import coil.groupId
import coil.versionName
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import java.net.URL
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradlePlugin.android)
        classpath(libs.gradlePlugin.kotlin)
        classpath(libs.gradlePlugin.mavenPublish)
    }
}

// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
}

extensions.configure<ApiValidationExtension> {
    ignoredProjects += arrayOf(
        "coil-sample-common",
        "coil-sample-compose",
        "coil-sample-view",
        "coil-test",
    )
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory by file("$rootDir/docs/api")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    group = project.groupId
    version = project.versionName

    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target("**/*.kt", "**/*.kts")
            ktlint(libs.ktlint.get().version)
            endWithNewline()
            indentWithSpaces()
            trimTrailingWhitespace()
        }
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion by 8
            skipDeprecated by true
            suppressInheritedMembers by true

            externalDocumentationLink {
                url by URL("https://developer.android.com/reference/")
            }
            externalDocumentationLink {
                url by URL("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okhttp/4.x/")
                packageListUrl by URL("https://colinwhite.me/okhttp3-package-list") // https://github.com/square/okhttp/issues/7338
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okio/3.x/okio/")
                packageListUrl by URL("https://square.github.io/okio/3.x/okio/okio/package-list")
            }
        }
    }

    plugins.withId("com.vanniktech.maven.publish.base") {
        group = project.groupId
        version = project.versionName

        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            pomFromGradleProperties()
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
    tasks.whenTaskAdded {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }

    // Work-around: https://issuetracker.google.com/issues/226095015
    plugins.withType(com.android.build.gradle.LibraryPlugin::class).configureEach {
        Class.forName("com.android.tools.analytics.AnalyticsSettings")
            .getMethod("setOptedIn", Boolean::class.java)
            .invoke(null, false)
    }
}
