import org.gradle.api.Project

// Verify Skiko versions match between coil-core and coil-compose-core.
val verifySkikoVersionsMatch by tasks.registering {
    group = "verification"
    description = "Ensures Skiko versions in coil-core and coil-compose-core match."

    doLast {
        val core = project(":coil-core")
        val composeCore = project(":coil-compose-core")

        core.evaluationDependsOn(core.path)
        composeCore.evaluationDependsOn(composeCore.path)

        fun requestedSkikoVersionFromJvmByOrigin(project: Project, originGroupPrefix: String): String {
            val configurationNames = listOf("jvmRuntimeClasspath", "jvmTestRuntimeClasspath")
            for (name in configurationNames) {
                val cfg = project.configurations.findByName(name) ?: continue
                // Force dependency graph calculation
                cfg.dependencies
                val result = cfg.incoming.resolutionResult
                result.allDependencies.forEach { dep ->
                    val resolved = dep as? org.gradle.api.artifacts.result.ResolvedDependencyResult ?: return@forEach
                    val from = resolved.from
                    val fromId = (from as? org.gradle.api.artifacts.result.ResolvedComponentResult)?.moduleVersion
                    val requested = resolved.requested as? org.gradle.api.artifacts.component.ModuleComponentSelector ?: return@forEach
                    if (fromId != null && fromId.group.startsWith(originGroupPrefix) && requested.group == "org.jetbrains.skiko") {
                        return requested.version
                    }
                }
            }
            error("Couldn't find requested Skiko JVM dependency in ${project.path} from '$originGroupPrefix' (checked ${configurationNames.joinToString()})")
        }

        val coreSkiko = requestedSkikoVersionFromJvmByOrigin(core, project.group.toString())
        val composeSkiko = requestedSkikoVersionFromJvmByOrigin(composeCore, "org.jetbrains.compose")

        if (coreSkiko != composeSkiko) {
            error("Skiko version mismatch: coil-core uses $coreSkiko, coil-compose-core uses $composeSkiko. Update libs.toml to use $composeSkiko.")
        }
    }
}

// Attach verification only to the root `check` task.
tasks.matching { it.name == "check" }.configureEach {
    dependsOn(tasks.named("verifySkikoVersionsMatch"))
}


