package xyz.gmitch215.gitle

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import java.io.File

internal val dependencies = mutableSetOf<GitDependency>()
internal lateinit var project: Project
internal lateinit var extension: GitleExtension

internal val logger = Logging.getLogger("gitle")
internal val rootDir: File
    get() = File(project.gradle.gradleUserHomeDir, "gitle")
internal lateinit var mavenLocal: File

internal class GitleException(message: String = "", cause: Throwable? = null) : RuntimeException(message, cause)