pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Add Dream Monet SDK repository
        maven {
            url = uri("https://nexus.synthraai.tech/repository/maven-releases/")
            credentials {
                username = settings.providers.gradleProperty("NEXUS_USERNAME").orNull
                password = settings.providers.gradleProperty("NEXUS_PASSWORD").orNull
            }
        }
        maven {
            url = uri("https://nexus.synthraai.tech/repository/maven-snapshots/")
            credentials {
                username = settings.providers.gradleProperty("NEXUS_USERNAME").orNull
                password = settings.providers.gradleProperty("NEXUS_PASSWORD").orNull
            }
        }
    }
}

rootProject.name = "DreamMonetSample"
include(":app")