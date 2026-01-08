package xyz.gmitch215.gitle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestGitDependency {

    companion object {

        private val project = ProjectBuilder.builder()
            .withName("TestGitDependency")
            .build()

        @BeforeAll
        @JvmStatic
        fun setup() {
            project.pluginManager.apply("xyz.gmitch215.gitle")
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun testHttp() {
        val d1 = http("http://example.com/user/repository.git")
        assertEquals("http://example.com/user/repository.git", d1.repositoryURL)

        val d2 = http("example.com/user/repository.git")
        assertEquals("http://example.com/user/repository.git", d2.repositoryURL)

        val d3 = http("example.com/user/repository")
        assertEquals("http://example.com/user/repository.git", d3.repositoryURL)
    }

    @Test
    fun testHttps() {
        val d1 = https("https://example.com/user/repository.git")
        assertEquals("https://example.com/user/repository.git", d1.repositoryURL)
        assertEquals("user/repository", d1.slug)
        assertNull(d1.checkout)
        assertEquals(UpdatePolicy.IF_OUT_OF_DATE, d1.updatePolicy)
        assertEquals("example.com-user-repository", d1.folderName)

        val d2 = https("example.com/user/repository.git")
        assertEquals("https://example.com/user/repository.git", d2.repositoryURL)
        assertEquals("user/repository", d2.slug)
        assertEquals("example.com-user-repository", d2.folderName)

        val d3 = https("example.com/user/repository", null, UpdatePolicy.ALWAYS)
        assertEquals("https://example.com/user/repository.git", d3.repositoryURL)
        assertEquals("user/repository", d3.slug)
        assertNull(d3.checkout)
        assertEquals(UpdatePolicy.ALWAYS, d3.updatePolicy)
        assertEquals("example.com-user-repository", d3.folderName)

        val d4 = https("example.com/user/repository", "other-branch", UpdatePolicy.IF_MISSING)
        assertEquals("https://example.com/user/repository.git", d4.repositoryURL)
        assertEquals("user/repository", d4.slug)
        assertEquals("other-branch", d4.checkout)
        assertEquals(UpdatePolicy.IF_MISSING, d4.updatePolicy)
        assertEquals("example.com-user-repository", d4.folderName)
    }

    @Test
    fun testSsh() {
        val d1 = ssh("user@example.com:user/repository.git")
        assertEquals("user", d1.user)
        assertEquals("example.com", d1.host)
        assertEquals("user/repository", d1.slug)
        assertNull(d1.checkout)

        val d2 = ssh("ssh://user@example.com:user/repository", "other-branch")
        assertEquals("user", d2.user)
        assertEquals("example.com", d2.host)
        assertEquals("user/repository", d2.slug)
        assertEquals("other-branch", d2.checkout)

        val d3 = ssh(github("gmitch215", "gitle"))
        assertEquals("git", d3.user)
        assertEquals("github.com", d3.host)
        assertEquals("gmitch215/gitle", d3.slug)
        assertNull(d3.checkout)

        val d4 = ssh(gitlab("gmitch215/gitle"))
        assertEquals("git", d4.user)
        assertEquals("gitlab.com", d4.host)
        assertEquals("gmitch215/gitle", d4.slug)
        assertNull(d4.checkout)

        assertThrows<IllegalArgumentException> { ssh("https://example.com/user/repository") }
        assertThrows<IllegalArgumentException> { ssh("ssh://example.com/user/repository") }
    }

    @Test
    fun testGitHub() {
        val dependency = github("gmitch215", "gitle")
        assertEquals("gmitch215", dependency.username)
        assertEquals("gitle", dependency.repoName)
        assertEquals("gmitch215/gitle", dependency.slug)

        assertTrue { dependency.repositoryURL.endsWith("github.com/gmitch215/gitle.git") }
    }

    @Test
    fun testGitLab() {
        val d1 = gitlab("gmitch215/gitle")
        assertEquals("gitlab.com", d1.host)
        assertEquals("gmitch215/gitle", d1.projectId)
        assertEquals("gmitch215/gitle", d1.slug)
        assertNull(d1.checkout)
        assertTrue { d1.repositoryURL.endsWith("gitlab.com/gmitch215/gitle.git") }
        assertEquals(UpdatePolicy.IF_OUT_OF_DATE, d1.updatePolicy)

        val d2 = gitlab("gmitch215/gitle", checkout = "other-branch")
        assertEquals("gitlab.com", d2.host)
        assertEquals("gmitch215/gitle", d2.projectId)
        assertEquals("gmitch215/gitle", d2.slug)
        assertEquals("other-branch", d2.checkout)
        assertTrue { d2.repositoryURL.endsWith("gitlab.com/gmitch215/gitle.git") }
        assertEquals(UpdatePolicy.IF_OUT_OF_DATE, d2.updatePolicy)

        val d3 = gitlab("gmitch215/gitle/submodule", checkout = "other-branch", updatePolicy = UpdatePolicy.ALWAYS)
        assertEquals("gitlab.com", d3.host)
        assertEquals("gmitch215/gitle/submodule", d3.projectId)
        assertEquals("gmitch215/gitle/submodule", d3.slug)
        assertEquals("other-branch", d3.checkout)
        assertTrue { d3.repositoryURL.endsWith("gitlab.com/gmitch215/gitle/submodule.git") }
        assertEquals(UpdatePolicy.ALWAYS, d3.updatePolicy)

        val d4 = gitlab("custom-host.com", "gmitch215/gitle", checkout = "other-branch", updatePolicy = UpdatePolicy.IF_MISSING)
        assertEquals("custom-host.com", d4.host)
        assertEquals("gmitch215/gitle", d4.projectId)
        assertEquals("gmitch215/gitle", d4.slug)
        assertEquals("other-branch", d4.checkout)
        assertTrue { d4.repositoryURL.endsWith("custom-host.com/gmitch215/gitle.git") }
        assertEquals(UpdatePolicy.IF_MISSING, d4.updatePolicy)

        assertThrows<IllegalArgumentException> { gitlab("gmitch215", "gitle") }
    }

    @Test
    fun testBitbucket() {
        val d1 = bitbucket("gmitch215", "gitle")
        assertEquals("gmitch215/gitle", d1.slug)

        assertTrue { d1.repositoryURL.endsWith("bitbucket.org/gmitch215/gitle.git") }
    }

    @Test
    fun testGitFunctions() {
        val d1 = github("CodeMC", "API")
        d1.clone()
        assertTrue { d1.isUpToDate() }

        val d2 = github("CodeMC", "API", checkout = "287e1f56b6b44e1b304bed18c8f3d55bec2e314f")
        d2.clone()
        assertTrue { d2.isUpToDate() }

        assertNotEquals(d1.folder, d2.folder)
    }

}