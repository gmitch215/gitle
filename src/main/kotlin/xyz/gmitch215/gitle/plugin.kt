package xyz.gmitch215.gitle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gradle.api.Project
import java.io.File

internal val dependencies = mutableSetOf<GitDependency>()
internal lateinit var project: Project
internal lateinit var extension: GitleExtension

internal val logger = KotlinLogging.logger("Gitle")
internal val rootDir: File
    get() = File(project.gradle.gradleUserHomeDir, "gitle")
internal lateinit var mavenLocal: File

internal class GitleException(message: String = "", cause: Throwable? = null) : RuntimeException(message, cause)