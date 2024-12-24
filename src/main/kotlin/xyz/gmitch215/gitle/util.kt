package xyz.gmitch215.gitle

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File
import java.net.URL

internal val s = File.separator
private val os = DefaultNativePlatform.getCurrentOperatingSystem().name.substringBefore(' ').lowercase().trim()

internal fun isOnline(): Boolean {
    if (extension.offlineMode) return false

    try {
        val url = URL("https://gradle.org")
        val connection = url.openConnection()
        connection.connect()

        return true
    } catch (e: Exception) {
        return false
    }
}

internal enum class ProjectType(
    val files: List<String>,
    val publish: String
) {

    MAVEN(
        listOf(
            "pom.xml",
            "dependency-reduced-pom.xml"
        ),
        "mvn${if (os == "windows") ".cmd" else ""} install"
    ),

    GRADLE(
        listOf(
            "build.gradle",
            "build.gradle.kts",
            "gradlew",
            "gradlew.bat",
            "settings.gradle",
            "settings.gradle.kts",
            "gradle.properties"
        ),
        ".${s}gradlew${if (os == "windows") ".bat" else ""} publishToMavenLocal"
    )

}

internal fun findProjectType(folder: File): ProjectType? {
    for (type in ProjectType.entries) {
        if (type.files.any { File(folder, it).exists() })
            return type
    }

    return null
}