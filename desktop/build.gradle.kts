import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":common"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.marzec.todo.MainKt"

        nativeDistributions {
            version = "1.0.0"
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ToDo App"

            windows {
                menu = true
                upgradeUuid = "ff6f9c4b-618c-4224-8ea3-ab56c0d94403"
            }
        }
    }
}
