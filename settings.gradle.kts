pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://repo.stringee.com/maven/releases/") } // Thêm Stringee repository
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Thay FAIL_ON_PROJECT_REPOS thành PREFER_SETTINGS
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.stringee.com/maven/releases/") } // Thêm Stringee repository
    }
}

rootProject.name = "DACS3"
include(":app")
