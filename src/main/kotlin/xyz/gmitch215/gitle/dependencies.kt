package xyz.gmitch215.gitle

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.net.URI
import java.security.MessageDigest

/**
 * Represents a dependency that is downloaded from a Git repository.
 */
abstract class GitDependency(
    /**
     * The full repository URL.
     */
    val repositoryURL: String,
    /**
     * The branch, tag, or commit to checkout.
     */
    val checkout: String? = null,
    /**
     * The update policy for the dependency.
     */
    val updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) {
    internal abstract val folderParent: String
    internal abstract val folderName: String

    // For time-based update policies - change hash based on repository and checkout
    private val idHash: String
        get() = String(MessageDigest.getInstance("MD5").digest("$repositoryURL-$checkout".toByteArray()))

    /**
     * The folder where the git repository is stored and cloned.
     */
    val folder: File
        get() = File(rootDir, "$folderParent/$folderName/${checkout ?: "default"}")

    internal val type: ProjectType?
        get() {
            if (!folder.exists()) return null
            return findProjectType(folder)
        }

    internal fun clone() {
        if (!isOnline()) {
            logger.error { "Failed to clone '$repositoryURL': No internet connection" }
            throw GitleException("Failed to clone '$repositoryURL': No internet connection")
        }

        logger.debug { "Starting clone of '$repositoryURL' in '${folder.absolutePath}'" }
        if (folder.exists()) {
            logger.debug { "Deleting existing folder '$folder'..." }
            folder.deleteRecursively()
        }

        folder.mkdirs()
        logger.info { "Cloning '$repositoryURL'..." }

        val builder = ProcessBuilder("git", "clone", repositoryURL, ".").directory(folder)
        if (extension.showOutput) builder.inheritIO()

        val process = builder.start()
        val cloneExit = process.waitFor()
        logger.debug { "Clone exit code: $cloneExit" }
        if (cloneExit != 0) {
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            logger.error { "Failed to clone repository '$repositoryURL' (exit code $cloneExit): $error" }
            throw GitleException("Failed to clone repository '$repositoryURL' (exit code $cloneExit): $error")
        }

        logger.info { "Cloned '$repositoryURL'" }

        if (checkout != null)
            checkout()
    }

    private fun checkout() {
        if (checkout == null) {
            logger.warn { "No checkout specified for '$repositoryURL'" }
            return
        }

        logger.info { "Checking out '$checkout'..." }

        val builder = ProcessBuilder("git", "checkout", checkout).directory(folder)
        if (extension.showOutput) builder.inheritIO()

        val process = builder.start()
        val checkoutExit = process.waitFor()
        logger.debug { "Checkout exit code: $checkoutExit" }
        if (checkoutExit != 0) {
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            logger.error { "Failed to checkout '$repositoryURL' at '$checkout' (exit code $checkoutExit): $error" }
            throw GitleException("Failed to checkout '$repositoryURL' at '$checkout' (exit code $checkoutExit): $error")
        }

        logger.info { "Checked out '$checkout'" }
    }

    internal fun update() {
        logger.info { "Updating '$repositoryURL'..." }

        val builder = ProcessBuilder("git", "pull").directory(folder)
        if (extension.showOutput) builder.inheritIO()

        val process = builder.start()
        val updateExit = process.waitFor()
        logger.debug { "Update exit code: $updateExit" }

        if (updateExit != 0) {
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            logger.error { "Failed to update repository '$repositoryURL' (exit code $updateExit): $error" }
            throw GitleException("Failed to update repository '$repositoryURL' (exit code $updateExit): $error")
        }
        
        if (checkout != null)
            checkout()

        logger.info { "Updated '$repositoryURL', publishing..." }
        publish()
        logger.info { "Published '$repositoryURL'" }
    }

    @VisibleForTesting
    internal fun isUpToDate(): Boolean {
        val builder = ProcessBuilder("git", "remote", "show", "origin").directory(folder)
        if (extension.showOutput) builder.inheritIO()

        val process = builder.start()
        val exitCode = process.waitFor()

        process.inputStream.bufferedReader().use { reader ->
            val output = reader.lineSequence()
            for (line in output) {
                when {
                    "up to date" in line -> return true
                    "behind" in line || "diverged" in line -> return false
                }
            }
        }

        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            throw GitleException("Failed to determine if repository was up to date (exit code $exitCode): $error")
        }
        
        return false
    }

    internal fun checkUpdate() {
        if (updatePolicy == UpdatePolicy.IF_MISSING) {
            if (!folder.exists()) {
                logger.debug { "'$repositoryURL' is missing, cloning..." }
                clone()

                logger.debug { "Updating '$repositoryURL'..." }
                update()
                return
            }
        }

        if (updatePolicy == UpdatePolicy.IF_OUT_OF_DATE) {
            if (!isOnline()) {
                logger.error { "Failed to check if '$repositoryURL' is out of date: No internet connection" }
                return
            }

            logger.debug { "Checking if '$repositoryURL' is out of date..." }
            if (isUpToDate()) {
                logger.debug { "'$repositoryURL' is up to date" }
                return
            }

            logger.debug { "'$repositoryURL' is out of date" }
            update()
            return
        }

        // Time-based update policy

        val lastUpdated = File(folder, "gitle-$idHash.lastUpdated")
        if (!lastUpdated.exists()) {
            logger.debug { "No last updated file found for '$repositoryURL'" }

            update()
            lastUpdated.createNewFile()
            lastUpdated.writeText(System.currentTimeMillis().toString())
            return
        }

        val lastUpdatedTime = lastUpdated.readText().toLong()
        val currentTime = System.currentTimeMillis()
        val timeSinceUpdate = currentTime - lastUpdatedTime

        if (updatePolicy.millis <= timeSinceUpdate) update()
    }

    internal fun publish() {
        if (type == null) {
            logger.warn { "No project type found for '$repositoryURL'" }
            return
        }

        val builder = ProcessBuilder(type!!.publish.split("\\s".toRegex())).directory(folder)
        if (extension.showOutput) builder.inheritIO()

        val process = builder.start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            logger.error { "Failed to publish '$repositoryURL' (exit code $exitCode): $error" }
            throw GitleException("Failed to publish '$repositoryURL' (exit code $exitCode): $error")
        }

        logger.info { "Published project '$repositoryURL'" }
    }

}

/**
 * Represents a dependency that is cloned over SSH.
 */
class SshDependency(
    /**
     * The URL for the host of the repository.
     */
    val host: String,
    /**
     * The username to use for the SSH connection.
     */
    val user: String,
    /**
     * The path to the repository on the host.
     */
    val slug: String,
    /**
     * The branch, tag, or commit to checkout.
     */
    checkout: String? = null,
    /**
     * The update policy for the dependency.
     */
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) : GitDependency("ssh://$user@$host:$slug", checkout, updatePolicy) {
    override val folderParent: String = "ssh"
    override val folderName: String
        get() = "$host-$user-${slug.replace('/', '-')}"
}

/**
 * Represents a dependency that is cloned over HTTP.
 */
class HttpDependency(
    /**
     * The URL of the repository.
     */
    val url: String,
    /**
     * The branch, tag, or commit to checkout.
     */
    checkout: String? = null,
    /**
     * The update policy for the dependency.
     */
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) : GitDependency("http://$url.git", checkout, updatePolicy) {
    override val folderParent: String = "http"
    override val folderName: String
        get() = url.replace('/', '-').removeSuffix(".git")
}

/**
 * Represents a dependency that is cloned over HTTPS.
 */
open class HttpsDependency(
    /**
     * The URL for the host of the repository.
     */
    val host: String,
    /**
     * The path to the repository on the host.
     */
    val slug: String,
    /**
     * The credentials used to clone the repository over HTTPS.
     */
    val credentials: String? = null,
    /**
     * The branch, tag, or commit to checkout.
     */
    checkout: String? = null,
    /**
     * The update policy for the dependency.
     */
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) : GitDependency("https://${if (!credentials.isNullOrBlank()) "$credentials@" else ""}$host/$slug.git", checkout, updatePolicy) {
    override val folderParent: String = "https"
    override val folderName: String
        get() = "$host-${slug.replace('/', '-')}"
}

/**
 * Represents a dependency that is downloaded from a GitHub repository.
 */
class GitHubDependency(
    /**
     * The username of the repository owner.
     */
    val username: String,
    /**
     * The name of the repository.
     */
    val repoName: String,
    /**
     * The access token to use for cloning the repository.
     */
    val accessToken: String? = null,
    /**
     * The branch, tag, or commit to checkout.
     */
    checkout: String? = null,
    /**
     * The update policy for the dependency.
     */
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) : HttpsDependency("github.com", "$username/$repoName", accessToken, checkout, updatePolicy) {
    override val folderParent: String = "github"
    override val folderName: String
        get() = "$username-$repoName"
}

/**
 * Represents a dependency that is downloaded from a GitLab repository.
 */
class GitLabDependency(
    /**
     * The URL of the GitLab server.
     */
    val server: String,
    /**
     * The project ID.
     */
    val projectId: String,
    /**
     * The username to use for the GitLab connection.
     */
    val loginUsername: String? = null,
    /**
     * The token used to clone the repository.
     */
    val token: String? = null,
    /**
     * The branch, tag, or commit to checkout.
     */
    checkout: String? = null,
    /**
     * The update policy for the dependency.
     */
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) : HttpsDependency(server, projectId, when {
    loginUsername != null && token != null -> "$loginUsername:$token"
    loginUsername != null -> loginUsername
    else -> null
}, checkout, updatePolicy) {
    override val folderParent: String = "gitlab"
    override val folderName: String
        get() = "$server-$projectId"
}

// Functions

/**
 * Adds a dependency to the list of dependencies to download.
 * @param dependency The dependency to add.
 * @see GitDependency
 */
fun DependencyHandler.import(dependency: GitDependency) {
    if (dependencies.any { it.repositoryURL == dependency.repositoryURL }) return
    dependencies.add(dependency)
}

/**
 * Adds a dependency to the list of dependencies to download.
 * @param url The full URL of the repository.
 */
fun DependencyHandler.import(url: String) {
    if (dependencies.any { it.repositoryURL == url }) return

    if (url.startsWith("ssh://"))
        import(ssh(url))
    else if (url.startsWith("http://")) {
        logger.warn { "Using insecure HTTP protocol for dependency: $url" }

        @Suppress("DEPRECATION")
        import(http(url))
    } else
        import(https(url))
}

/**
 * Adds multiple dependencies to the list of dependencies to download.
 * @param dependencies The dependencies to add.
 */
fun DependencyHandler.import(vararg dependencies: GitDependency) = dependencies.forEach(::import)

/**
 * Adds multiple dependencies to the list of dependencies to download.
 * @param urls The URLs of the repositories to add.
 */
fun DependencyHandler.import(vararg urls: String) = urls.forEach(::import)

/**
 * Adds multiple dependencies to the list of dependencies to download.
 * @param objects The dependencies to add.
 */
fun DependencyHandler.import(vararg objects: Any) {
    objects.forEach {
        when (it) {
            is GitDependency -> import(it)
            is String -> import(it)
            else -> throw IllegalArgumentException("Invalid dependency object: $it")
        }
    }
}

/**
 * Creates a new HTTP Git dependency.
 * @param url The full URL of the repository.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The HTTP dependency.
 * @see HttpDependency
 */
@JvmOverloads
@Deprecated("Use the HTTPS dependency instead", level = DeprecationLevel.WARNING)
fun http(
    url: String,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
): HttpDependency {
    val url0 = url.removePrefix("http://").removeSuffix(".git")
    return HttpDependency(url0, checkout, updatePolicy)
}

/**
 * Creates a new HTTPS Git dependency.
 * @param url The full URL of the repository.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The HTTPS dependency.
 * @see HttpsDependency
 */
fun https(
    url: String,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
): HttpsDependency {
    val url0 = url.removePrefix("https://").removeSuffix(".git")
    if ('/' !in url0) throw IllegalArgumentException("Invalid URL: $url0")

    val (host0, slug) = url0.split('/', limit = 2)
    val (host, credentials) = if ("@" in host0) {
        val (creds, host1) = host0.split('@', limit = 2)
        Pair(host1, creds)
    } else Pair(host0, null)

    when (host) {
        "github.com" -> return github(slug, credentials, checkout, updatePolicy)
        "gitlab.com" -> {
            val (loginUsername, token) = if (credentials != null) {
                val (login, token0) = credentials.split(':', limit = 2)
                Pair(login, token0)
            } else Pair(null, null)

            return gitlab(slug, loginUsername, token, checkout, updatePolicy)
        }
    }

    return https(host, slug, credentials, checkout, updatePolicy)
}

/**
 * Creates a new HTTPS Git dependency.
 * @param host The URL for the host of the repository.
 * @param slug The path to the repository on the host.
 * @param credentials The credentials used to clone the repository over HTTPS, if any.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The HTTPS dependency.
 * @see HttpsDependency
 */
fun https(
    host: String,
    slug: String,
    credentials: String? = null,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) = HttpsDependency(host, slug, credentials, checkout, updatePolicy)

/**
 * Creates a new GitHub dependency.
 *
 * Clones over HTTPS.
 * @param slug The slug of the repository in the format `username/repo`.
 * @param accessToken The access token to use for cloning the repository. If not specified, tries to fund an environment variable named `GITHUB_TOKEN`, or clones anonymously.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The GitHub dependency.
 * @see GitHubDependency
 */
fun github(
    slug: String,
    accessToken: String? = null,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
): GitHubDependency {
    if ('/' !in slug) throw IllegalArgumentException("Invalid slug: $slug")

    val (username, repoName) = slug.split('/', limit = 2)
    return github(username, repoName, accessToken, checkout, updatePolicy)
}

/**
 * Creates a new GitHub dependency.
 *
 * Clones over HTTPS.
 * @param username The username of the repository owner.
 * @param repoName The name of the repository.
 * @param accessToken The access token to use for cloning the repository. If not specified, tries to fund an environment variable named `GITHUB_TOKEN`, or clones anonymously.
 * @param checkout The branch, tag, or commit to checkout.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The GitHub dependency.
 * @see GitHubDependency
 */
fun github(
    username: String,
    repoName: String,
    accessToken: String? = null,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
): GitHubDependency = GitHubDependency(username, repoName, accessToken ?: System.getenv("GITHUB_TOKEN"), checkout, updatePolicy)

/**
 * Creates a new GitLab dependency with the default server.
 *
 * Clones over HTTPS.
 * @param projectId The project slug or ID.
 * @param loginUsername The username to use for the GitLab connection, if any.
 * @param token The token used to clone the repository, if any. If specified, [loginUsername] must also be specified.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The GitLab dependency.
 * @see GitLabDependency
 */
fun gitlab(
    projectId: String,
    loginUsername: String? = null,
    token: String? = null,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) = gitlab("gitlab.com", projectId, loginUsername, token, checkout, updatePolicy)

/**
 * Creates a new GitLab dependency with a custom server.
 *
 * Clones over HTTPS.
 * @param server The URL of the GitLab server.
 * @param projectId The project slug or ID.
 * @param loginUsername The username to use for the GitLab connection, if any.
 * @param token The token used to clone the repository, if any. If specified, [loginUsername] must also be specified.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The GitLab dependency.
 * @see GitLabDependency
 */
fun gitlab(
    server: String,
    projectId: String,
    loginUsername: String? = null,
    token: String? = null,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
): GitLabDependency {
    if ('/' !in projectId) throw IllegalArgumentException("Invalid slug: $projectId")
    if (!server.contains(".")) throw IllegalArgumentException("Invalid server URL: $server")

    // Validate server URL
    URI.create(server)

    return GitLabDependency(server, projectId, loginUsername, token, checkout, updatePolicy)
}

/**
 * Creates a new SSH Git dependency.
 * @param url The full URL of the repository.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The SSH dependency.
 * @see SshDependency
 */
fun ssh(
    url: String,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
): SshDependency {
    val url0 = url.removePrefix("ssh://")

    if (':' !in url0) throw IllegalArgumentException("Invalid SSH URL: $url0")
    if ('@' !in url0) throw IllegalArgumentException("Invalid SSH URL: $url0")

    val (user, host0) = url0.split('@', limit = 2)
    val (host, slug0) = host0.split(':', limit = 2)

    return ssh(host, user, slug0.removeSuffix(".git"), checkout, updatePolicy)
}

/**
 * Creates a new SSH Git dependency.
 * @param host The URL for the host of the repository.
 * @param user The username to use for the SSH connection.
 * @param slug The path to the repository on the host.
 * @param checkout The branch, tag, or commit to checkout. If not specified, the default branch is used.
 * @param updatePolicy The update policy for the dependency. Default is [UpdatePolicy.IF_OUT_OF_DATE].
 * @return The SSH dependency.
 * @see SshDependency
 */
fun ssh(
    host: String,
    user: String,
    slug: String,
    checkout: String? = null,
    updatePolicy: UpdatePolicy = extension.defaultUpdatePolicy
) = SshDependency(host, user, slug, checkout, updatePolicy)

/**
 * Creates a new SSH Dependency from a GitHub dependency.
 * @param github The GitHub dependency.
 * @return The SSH dependency counterpart.
 * @see SshDependency
 * @see GitHubDependency
 */
fun ssh(github: GitHubDependency) = ssh("github.com", "git", "${github.username}/${github.repoName}", github.checkout, github.updatePolicy)

/**
 * Creates a new SSH Dependency from a GitLab dependency.
 *
 * This uses the default GitLab server. If you want to use a custom server, use the default [ssh] function.
 * @param gitlab The GitLab dependency.
 * @return The SSH dependency counterpart.
 * @see SshDependency
 * @see GitLabDependency
 */
fun ssh(gitlab: GitLabDependency) = ssh("gitlab.com", "git", gitlab.projectId, gitlab.checkout, gitlab.updatePolicy)
