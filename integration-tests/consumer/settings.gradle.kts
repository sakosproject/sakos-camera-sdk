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
        maven { url = uri("../../build/local-maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "sakos-camera-sdk-consumer"
include(":app")
