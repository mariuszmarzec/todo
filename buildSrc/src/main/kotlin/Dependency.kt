val kotlinVersion = "1.4.10"
val coreKtxVersion = "1.3.2"
val appCompatVersion = "1.2.0"
val materialVersion = "1.1.0"
val androidGradlepluginVersion = "4.0.1"

object Dependency {

    val androidBuildPlugin = "com.android.tools.build:gradle:${androidGradlepluginVersion}"
    val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"

    val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    val androidxCoreKtx = "androidx.core:core-ktx:$coreKtxVersion"
    val androidxAppCompat = "androidx.appcompat:appcompat:$appCompatVersion"
    val androidMaterial = "com.google.android.material:material:$materialVersion"
}

object Config {
    object Android {
        val compileSdkVersion = 29
        val applicationId = "com.marzec.cheatday"
        val minSdkVersion = 29
        val targetSdkVersion = 29
        val versionCode = 1
        val versionName = "1.0"
    }
}