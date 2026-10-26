import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.application")
    id("io.gitlab.arturbosch.detekt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.marzec.todo"

    compileSdk = libs.versions.compileSdk.get().toInt()
    compileSdkMinor = 0
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
            val propertiesFile = File("${rootDir}/local.properties")
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
            isDebuggable = true
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.fcm)
    testImplementation(libs.junit)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    compilerOptions {
        jvmTarget.set(JVM_17)
        freeCompilerArgs.addAll("-Xskip-prerelease-check")
    }
}

detekt {
    source.setFrom("src/main/kotlin")
    config.setFrom("../config/detekt/detekt.yml")
}
