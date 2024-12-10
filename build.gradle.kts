plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradle.plugin-publish") version "1.3.0"

    `maven-publish`
    signing
}

group = "xyz.gmitch215"
version = "0.1.0"

gradlePlugin {
    website.set("https://github.com/gmitch215/gitle")
    vcsUrl.set("https://github.com/gmitch215/gitle.git")

    plugins {
        create("gitle") {
            id = "xyz.gmitch215.gitle"
            displayName = "Gitle"
            description = "Download artifacts from Git Repositories"
            implementationClass = "xyz.gmitch215.gitle.GitlePlugin"
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/gmitch215/gitle")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/gmitch215/gitle/blob/master/LICENSE")
                    }
                }
            }

            from(components["java"])
        }
    }

    repositories {
        maven {
            credentials {
                username = System.getenv("JENKINS_USERNAME")
                password = System.getenv("JENKINS_PASSWORD")
            }

            url = uri("https://repo.calcugames.xyz/repository/maven-releases/")
        }
    }
}