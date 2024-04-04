import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.libsDirectory

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("io.gitlab.arturbosch.detekt")
     // TODO wait for stable 7.3 AGP
//    id("com.google.gms.google-services")
}

android {
    namespace = "com.marzec.todo"

    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        applicationId = "com.marzec.todo"
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            val propertiesFile = File("local.properties")
            if (propertiesFile.exists()) {
                properties.load(FileInputStream(propertiesFile))
                storeFile = file(properties.getProperty("storeFile"))
                keyAlias = properties.getProperty("keyAlias")
                storePassword = properties.getProperty("storePassword")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidxAppCompat)
    implementation(libs.androidMaterial)
    implementation(libs.activityCompose)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    implementation(compose.foundation)

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf(
        *kotlinOptions.freeCompilerArgs.toTypedArray(),
        "-Xskip-prerelease-check")
}

detekt {
    source = files(
        "src/main/kotlin"
    )

    config = files("../config/detekt/detekt.yml")
}
