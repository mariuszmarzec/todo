plugins {
    id("com.android.library")
    // id(libs.plugins.compose-compiler.pluginId)  // Removed - use alias instead
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.kotlinStdlib)
                implementation(libs.quickMvi)
                implementation(libs.quickMvi.compose)
                implementation(libs.coroutineCore)
                implementation(libs.kotlinDateTime)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutineTest)
                implementation(libs.mockk)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.coroutineTest)
                implementation(libs.mockk)
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}
