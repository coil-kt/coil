import coil.by
import coil.groupId
import coil.versionName
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

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

    configurations.classpath {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                libs.ktlint.get().module.group -> useVersion(libs.versions.ktlint.get())
            }
        }
    }
}

// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
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

    apply(plugin = "org.jmailen.kotlinter")

    kotlinter {
        disabledRules = arrayOf(
            "annotation",
            "argument-list-wrapping",
            "filename",
            "indent",
            "max-line-length",
            "parameter-list-wrapping",
            "spacing-between-declarations-with-annotations",
            "wrapping",
        )
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

    // Uninstall test APKs after running instrumentation tests.
    tasks.whenTaskAdded {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }
}
