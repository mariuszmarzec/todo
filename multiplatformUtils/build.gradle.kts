import org.jetbrains.compose.ComposePlugin.DesktopComponentsDependencies
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("kotlinx-atomicfu")
}


kotlin {
    android()
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
                api(Dependency.kotlinStdlib)
                api(Dependency.coroutineCore)
                api(Dependency.ktorClient)
                api(Dependency.okHttpClientLogger)
                api(Dependency.ktorSerialization)
                api(Dependency.ktorContentNegotiation)
                api(Dependency.serializationJson)
                implementation(Dependency.kotlinDateTime)
            }
        }
        val desktopTest by getting {
            dependencies {
                api(project(":multiplatformUtils"))
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.uiTestJUnit4)
                implementation(kotlin("test"))
                implementation(Dependency.coroutineTest)
                implementation(Dependency.mockk)
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.appcompat:appcompat:1.1.0")
                api("androidx.core:core-ktx:1.3.1")
                api(Dependency.ktorOkHttpClient)
                implementation(Dependency.imageLoader)
                implementation(Dependency.exoPlayer)

                // data store
                api("androidx.datastore:datastore-preferences:${datastore_version}")
                api("androidx.datastore:datastore:${datastore_version}")

            }
        }
        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(Dependency.ktorOkHttpClient)
                api(Dependency.vlc)
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
                implementation(Dependency.coroutineTest)
                implementation(Dependency.mockk)
            }
        }
    }
}

android {

    namespace = "com.marzec"

    compileSdk = Config.Android.compileSdkVersion

    defaultConfig {
        minSdk = Config.Android.minSdkVersion
        targetSdk = Config.Android.targetSdkVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
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
