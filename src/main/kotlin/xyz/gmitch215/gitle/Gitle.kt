package xyz.gmitch215.gitle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The main Gitle plugin class.
 */
class Gitle : Plugin<Project> {

    override fun apply(target: Project) {
        project = target
        extension = target.extensions.create("gitle", GitleExtension::class.java)

        target.repositories.mavenLocal()

        target.tasks.register("checkGitle", CheckGitleTask::class.java)

        if (target.tasks.any { it.name == "classes" })
            target.tasks.getByName("classes").dependsOn("checkGitle")

        if (!rootDir.exists()) rootDir.mkdirs()
    }

}