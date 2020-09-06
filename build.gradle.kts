import coil.by
import coil.groupId
import coil.versionName
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URL

buildscript {
    apply(from = "buildSrc/extra.gradle.kts")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(rootProject.extra["androidPlugin"].toString())
        classpath(rootProject.extra["kotlinPlugin"].toString())
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.14.2")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.0")
        classpath("org.jetbrains.kotlinx:binary-compatibility-validator:0.5.0")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.0.0")
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

        // https://github.com/Kotlin/dokka/issues/41
        jcenter {
            content {
                includeModule("org.jetbrains.dokka", "dokka-fatjar")
            }
        }
    }

    group = project.groupId
    version = project.versionName

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension>("ktlint") {
        version by "0.41.0"
        disabledRules by setOf("indent", "max-line-length")
    }

    // https://github.com/JLLeitschuh/ktlint-gradle/issues/458
    @Suppress("UnstableApiUsage")
    configurations.named("ktlint").configure {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("com.pinterest:ktlint")).with(variant(module("com.pinterest:ktlint:0.41.0")) {
                    attributes {
                        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.EXTERNAL))
                    }
                })
            }
        }
    }

    tasks.withType<DokkaTask>().configureEach {
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(8)
                reportUndocumented.set(false)
                skipDeprecated.set(true)
                skipEmptyPackages.set(true)
                outputDirectory.set(file("$rootDir/docs/api"))

                externalDocumentationLink {
                    url.set(URL("https://developer.android.com/reference/"))
                    packageListUrl.set(URL("https://developer.android.com/reference/androidx/package-list"))
                }
                externalDocumentationLink {
                    url.set(URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-android/"))
                    packageListUrl.set(URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-android/package-list"))
                }
                externalDocumentationLink {
                    url.set(URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/"))
                    packageListUrl.set(URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/package-list"))
                }
                externalDocumentationLink {
                    url.set(URL("https://square.github.io/okhttp/3.x/okhttp/"))
                    packageListUrl.set(URL("https://square.github.io/okhttp/3.x/okhttp/package-list"))
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
