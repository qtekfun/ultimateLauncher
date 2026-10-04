pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
    }
}
rootProject.name = "ultimatelauncher"
include(":app", ":platform-stubs")
for (m in listOf("iconloaderlib", "animationlib", "msdllib", "dynamiccolors", "usertypelib")) {
    include(":$m"); project(":$m").projectDir = file("systemui-libs/$m")
}
include(":widgetpicker")
project(":widgetpicker").projectDir = file("launcher3-base/modules/widgetpicker")
project(":widgetpicker").buildFileName = "ul-build.gradle" // build.gradle.kts de AOSP no es utilizable fuera de su árbol
