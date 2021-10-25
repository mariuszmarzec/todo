pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":common")
include(":app")
include(":desktop")
rootProject.name = "Todo"
