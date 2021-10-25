import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("io.gitlab.arturbosch.detekt")
}

android {
    compileSdk = Config.Android.compileSdkVersion
    defaultConfig {
        minSdk = Config.Android.minSdkVersion
        targetSdk = Config.Android.targetSdkVersion
        applicationId = Config.Android.applicationId
        versionCode = Config.Android.versionCode
        versionName = Config.Android.versionName
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
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(Dependency.androidxAppCompat)
    implementation(Dependency.androidMaterial)
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.uiTooling)
    implementation(compose.foundation)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
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
