import org.jetbrains.compose.compose

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
            useJUnitPlatform()
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
                api(Dependency.ktorSerialization)
                api(Dependency.serializationJson)
                implementation(Dependency.kotlinDateTime)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(Dependency.coroutineTest)
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.appcompat:appcompat:1.1.0")
                api("androidx.core:core-ktx:1.3.1")
                api(Dependency.ktorOkHttpClient)
                implementation(Dependency.imageLoader)

                // data store
                api("androidx.datastore:datastore-preferences:${datastore_version}")
                api("androidx.datastore:datastore:${datastore_version}")

            }
        }
        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(Dependency.ktorOkHttpClient)
            }
        }
    }
}

android {

    namespace = "com.marzec"

    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(30)
        targetSdkVersion(30)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf(
        *kotlinOptions.freeCompilerArgs.toTypedArray(),
        "-Xallow-jvm-ir-dependencies",
        "-Xskip-prerelease-check"
    )
}
