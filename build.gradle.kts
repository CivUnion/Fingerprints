plugins {
    `java-library`
    `maven-publish`
	id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Temporary hack:
// Remove the root build directory
gradle.buildFinished {
	project.buildDir.deleteRecursively()
}

allprojects {
	group = rootProject.group
	version = rootProject.version
	description = rootProject.description
}

subprojects {
	apply(plugin = "java-library")
	apply(plugin = "maven-publish")
	apply(plugin = "com.github.johnrengelman.shadow")

	java {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(17))
			withJavadocJar()
			withSourcesJar()
		}
	}

	tasks {
		compileJava {
			options.encoding = Charsets.UTF_8.name()
			options.release.set(17)
		}

		processResources {
			filteringCharset = Charsets.UTF_8.name()
			filesMatching("**/plugin.yml") {
				expand(project.properties)
			}
		}
	}

	repositories {
		mavenCentral()
		maven {
			url = uri("https://nexus.civunion.com/repository/maven-releases/")
		}
	}

	publishing {
		repositories {
			maven {
				url = uri("https://nexus.civunion.com/repository/maven-releases/")
				credentials {
					username = System.getenv("REPO_USERNAME")
					password = System.getenv("REPO_PASSWORD")
				}
			}
		}
		publications {
			register<MavenPublication>("main") {
				from(components["java"])
			}
		}
	}
}
