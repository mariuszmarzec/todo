import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdkVersion(Config.Android.compileSdkVersion)
    defaultConfig {
        applicationId = Config.Android.applicationId
        minSdkVersion(Config.Android.minSdkVersion)
        targetSdkVersion(Config.Android.targetSdkVersion)
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
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(Dependency.kotlinStdlib)
    implementation(Dependency.androidxCoreKtx)
    implementation(Dependency.androidxAppCompat)
    implementation(Dependency.androidMaterial)
}