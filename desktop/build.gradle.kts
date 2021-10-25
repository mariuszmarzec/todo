import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("io.gitlab.arturbosch.detekt")
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
        println(System.getenv("JDK_15"))
        javaHome = System.getenv("JDK_15")

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

detekt {
    source = files(
        "src/jvmMain/kotlin"
    )

    config = files("../config/detekt/detekt.yml")
}
