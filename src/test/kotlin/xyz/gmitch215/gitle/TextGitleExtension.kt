package xyz.gmitch215.gitle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextGitleExtension {

    private val project = ProjectBuilder.builder()
        .withName("TestGitleExtension")
        .build()

    @Test
    fun test() {
        project.pluginManager.apply("xyz.gmitch215.gitle")

        val extension = project.extensions.getByType(GitleExtension::class.java)
        assertNotNull(extension)

        extension.offlineMode = true

        assert(extension.offlineMode)
        assertFalse(isOnline())

        extension.offlineMode = false

        assert(!extension.offlineMode)
        assertTrue(isOnline())
    }

}