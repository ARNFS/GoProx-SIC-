pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )

    repositories {
        google()
        mavenCentral()

        // Agora Maven CDN
        maven {
            url = uri(
                "https://download.agora.io/maven/"
            )
        }

        // JitPack
        maven {
            url = uri(
                "https://jitpack.io"
            )
        }
    }
}

rootProject.name = "GoProx3"

include(":app")
