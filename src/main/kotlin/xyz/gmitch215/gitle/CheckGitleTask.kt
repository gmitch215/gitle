package xyz.gmitch215.gitle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.annotations.VisibleForTesting

open class CheckGitleTask : DefaultTask() {

    init {
        group = "gitle"
        description = "Checks all of the registered Git dependencies"
    }

    @get:VisibleForTesting
    @get:Internal
    internal val gitDependencies: List<GitDependency>
        get() = dependencies.toList()

    @TaskAction
    fun checkGitle() {
        dependencies.forEach { dependency ->
            if (!dependency.folder.exists()) {
                logger.info("Cloning '$dependency'...")
                dependency.clone()
                dependency.publish()
                return@forEach
            }

            dependency.checkUpdate()
        }
    }
}