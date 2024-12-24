package xyz.gmitch215.gitle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class TestGitle {

    private val project = ProjectBuilder.builder()
        .withName("TestGitle")
        .build()

    @Test
    fun test() {
        project.pluginManager.apply("xyz.gmitch215.gitle")

        val extension = project.extensions.getByType(GitleExtension::class.java)
        assertNotNull(extension)

        extension.showOutput = true

        project.dependencies.import(github("CodeMC", "API"))
        project.dependencies.import("https://github.com/gmitch215/CTCore")

        project.tasks.getByName("checkGitle") {
            val task = it as? CheckGitleTask ?: fail("Task is not CheckGitleTask")

            assertEquals(2, task.gitDependencies.size)
            task.checkGitle()
        }
    }

}