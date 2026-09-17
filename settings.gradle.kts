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
    }
}

rootProject.name = "sakos-camera-sdk"

include(":safety-core")
include(":safety-opennsfw2")
include(":capture-camerax")
include(":capture-video")
include(":sample-app")
