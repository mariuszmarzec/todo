import org.jetbrains.compose.compose
import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("com.codingfeline.buildkonfig")
    id("io.gitlab.arturbosch.detekt")
}

val properties: Properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())
val environment = properties.getProperty("environment")
val prodApiUrl = properties.getProperty("prod.apiUrl")
val prodAuthHeader = properties.getProperty("prod.authHeader")
val testApiUrl = properties.getProperty("test.apiUrl")
val testAuthHeader = properties.getProperty("test.authHeader")

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
                api(project(":multiplatformUtils"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(libs.kotlinStdlib)
                api(libs.coroutineCore)
                api(libs.ktorClient)
                api(libs.ktorSerialization)
                api(libs.serializationJson)
                implementation(libs.kotlinDateTime)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutineTest)
            }
        }
        named("androidMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api("androidx.appcompat:appcompat:1.1.0")
                api("androidx.core:core-ktx:1.3.1")
                api(libs.ktorOkHttpClient)

                // data store
                api(libs.datastore.preferences)
                api(libs.datastore)

                implementation(libs.compose.viewModel)
            }
        }
        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(libs.ktorOkHttpClient)
            }
        }
    }
}

android {

    namespace = "com.marzec.todo.common"

    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
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
        "-Xskip-prerelease-check")
}

buildkonfig {
    packageName = "com.marzec.todo"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "ENVIRONMENT", environment)
        buildConfigField(FieldSpec.Type.STRING, "PROD_API_URL", prodApiUrl)
        buildConfigField(FieldSpec.Type.STRING, "PROD_AUTH_HEADER", prodAuthHeader)
        buildConfigField(FieldSpec.Type.STRING, "TEST_API_URL", testApiUrl)
        buildConfigField(FieldSpec.Type.STRING, "TEST_AUTH_HEADER", testAuthHeader)
    }
}

detekt {
    source = files(
        "src/androidMain/kotlin",
        "src/commonMain/kotlin",
        "src/jvmAndAndroidMain/kotlin",
        "src/desktopTest/kotlin",
        "src/desktopMain/kotlin"
    )

    config = files("../config/detekt/detekt.yml")
}
