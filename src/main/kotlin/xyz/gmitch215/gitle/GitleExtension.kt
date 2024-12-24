package xyz.gmitch215.gitle

/**
 * The main Gitle entry point.
 */
open class GitleExtension {
    /**
     * The list of repositories to clone.
     */
    val repositories: List<String>
        get() = dependencies.map(GitDependency::repositoryURL)

    /**
     * A copy of the list of dependencies.
     */
    val gitDependencies: List<GitDependency>
        get() = dependencies.toList()

    /**
     * Whether to operate in offline mode.
     */
    var offlineMode = false

    /**
     * Whether to show the output of the commands run.
     */
    var showOutput = false

    /**
     * The default update policy for all repositories.
     */
    var defaultUpdatePolicy: UpdatePolicy = UpdatePolicy.IF_OUT_OF_DATE
}