package coil3

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult

class VerifySkikoVersionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.register(
            "verifySkikoVersionsMatch",
            VerifySkikoVersionsTask::class.java,
        ) {
            group = "verification"
            description = "Ensures Skiko versions in coil-core and coil-compose-core match."

            val coreRequested = target.provider {
                requestedSkikoVersionFromJvmByOrigin(
                    targetProject = target.project(":coil-core"),
                    originGroupPrefix = target.group.toString(),
                )
            }
            val composeRequested = target.provider {
                requestedSkikoVersionFromJvmByOrigin(
                    targetProject = target.project(":coil-compose-core"),
                    originGroupPrefix = "org.jetbrains.compose",
                )
            }
            coreRequestedSkikoVersion.set(coreRequested)
            composeRequestedSkikoVersion.set(composeRequested)
        }

        // Attach verification only to the root `check` task.
        target.tasks.matching { it.name == "check" }.configureEach {
            dependsOn(target.tasks.named("verifySkikoVersionsMatch"))
        }
    }

    private fun requestedSkikoVersionFromJvmByOrigin(
        targetProject: Project,
        originGroupPrefix: String,
    ): String {
        val configurationNames = listOf("jvmRuntimeClasspath", "jvmTestRuntimeClasspath")
        for (name in configurationNames) {
            val cfg = targetProject.configurations.findByName(name) ?: continue

            // Force dependency graph calculation.
            cfg.dependencies

            val result = cfg.incoming.resolutionResult
            for (dep in result.allDependencies) {
                val resolved = dep as? ResolvedDependencyResult ?: continue
                val fromId = resolved.from.moduleVersion
                val requested = resolved.requested as? ModuleComponentSelector ?: continue
                if (
                    fromId != null &&
                    fromId.group.startsWith(originGroupPrefix) &&
                    requested.group == "org.jetbrains.skiko"
                ) {
                    return requested.version
                }
            }
        }
        error(
            "Couldn't find requested Skiko JVM dependency in ${targetProject.path} from " +
                "'$originGroupPrefix' (checked ${configurationNames.joinToString()}).",
        )
    }
}
