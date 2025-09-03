import org.jetbrains.compose.ComposePlugin.DesktopComponentsDependencies
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.atomicfu")
}


kotlin {
    androidTarget()
    jvm("desktop") {
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation(compose.components.resources)
                api(libs.kotlinStdlib)
                api(libs.coroutineCore)
                api(libs.ktorClient)
                api(libs.okHttpClientLogger)
                api(libs.ktorSerialization)
                api(libs.ktorContentNegotiation)
                api(libs.serializationJson)
                api(libs.quickMvi)
                api(libs.quickMvi.compose)
                implementation(libs.kotlinDateTime)
                implementation(libs.compose.icons)
            }
        }
        val desktopTest by getting {
            dependencies {
                api(project(":multiplatformUtils"))
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.uiTestJUnit4)
                implementation(kotlin("test"))
                implementation(libs.coroutineTest)
                implementation(libs.mockk)
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.core:core-ktx:1.3.1")
                implementation(libs.androidxAppCompat)
                implementation(libs.androidMaterial)
                implementation(libs.activityCompose)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.uiTooling)
                implementation(compose.foundation)
                api(libs.ktorOkHttpClient)
                implementation(libs.imageLoader)
                implementation(libs.exoPlayer)
                // data store
                api(libs.datastore.preferences)
                api(libs.datastore)
            }
        }
        val androidUnitTest by getting

        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(libs.ktorOkHttpClient)
                api(libs.vlc)
                implementation(compose.desktop.currentOs)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(DesktopComponentsDependencies.animatedImage)
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

android {

    namespace = "com.marzec"

    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")

            res.srcDirs("src/androidMain/res")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf(
        *kotlinOptions.freeCompilerArgs.toTypedArray(),
        "-Xallow-jvm-ir-dependencies",
        "-Xskip-prerelease-check"
    )
}
