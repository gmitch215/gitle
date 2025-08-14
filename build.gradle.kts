import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.gradle.plugin-publish") version "1.3.1"
    id("com.vanniktech.maven.publish") version "0.34.0"

    jacoco
    `maven-publish`
    signing
}

group = "xyz.gmitch215"
version = "0.1.1"
description = "Download artifacts from Git Repositories"

gradlePlugin {
    website.set("https://github.com/gmitch215/gitle")
    vcsUrl.set("https://github.com/gmitch215/gitle.git")

    plugins {
        create("gitle") {
            id = "xyz.gmitch215.gitle"
            displayName = "Gitle"
            description = "Download artifacts from Git Repositories"
            implementationClass = "xyz.gmitch215.gitle.Gitle"
            tags = listOf("git", "download", "repository", "dependency")
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))

    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
    runtimeOnly("ch.qos.logback:logback-core:1.5.18")
    implementation("io.github.oshai:kotlin-logging:7.0.12")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withSourcesJar()
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            csv.required.set(false)

            xml.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("jacoco.xml"))

            html.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKey != null && signingPassword != null)
        useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications)
}

dokka {
    moduleName.set("gitle")
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/gmitch215/gitle/blob/main")
            remoteLineSuffix.set("#L")
        }

        pluginsConfiguration.html {
            footerMessage.set("Copyright (c) Gregory Mitchell")
        }
    }
}

mavenPublishing {
    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set("gitle")
        description.set(project.description)
        url.set("https://github.com/gmitch215/gitle")
        inceptionYear.set("2024")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id = "gmitch215"
                name = "Gregory Mitchell"
                email = "me@gmitch215.xyz"
            }
        }

        scm {
            connection = "scm:git:git://github.com/gmitch215/gitle.git"
            developerConnection = "scm:git:ssh://github.com/gmitch215/gitle.git"
            url = "https://github.com/gmitch215/gitle"
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)
    signAllPublications()
}
