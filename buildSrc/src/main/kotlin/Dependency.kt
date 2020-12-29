val kotlinVersion = "1.4.21"
val ktorVersion = "1.4.3"
val serializationVersion = "1.0.1"
val coreKtxVersion = "1.3.2"
val appCompatVersion = "1.2.0"
val materialVersion = "1.1.0"
val androidGradlepluginVersion = "7.0.0-alpha03"
val composeVersion = "1.0.0-alpha09"

object Dependency {

    val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion"

    val ktorClient = "io.ktor:ktor-client-core:$ktorVersion"
    val ktorOkHttpClient = "io.ktor:ktor-client-okhttp:$ktorVersion"
    val ktorSerialization = "io.ktor:ktor-client-serialization-jvm:$ktorVersion"

    val androidBuildPlugin = "com.android.tools.build:gradle:${androidGradlepluginVersion}"
    val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
    val serializationGradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"

    val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    val androidxCoreKtx = "androidx.core:core-ktx:$coreKtxVersion"
    val androidxAppCompat = "androidx.appcompat:appcompat:$appCompatVersion"
    val androidMaterial = "com.google.android.material:material:$materialVersion"

    object Compose {
        val composeUi = "androidx.compose.ui:ui:$composeVersion"
        val composeCompiler = "androidx.compose.compiler:compiler:$composeVersion"
        val composeTooling = "androidx.compose.ui:ui-tooling:$composeVersion"
        val composeFoundation = "androidx.compose.foundation:foundation:$composeVersion"
        val composeMaterial = "androidx.compose.material:material:$composeVersion"
        val composeMaterialIcons = "androidx.compose.material:material-icons-core:$composeVersion"
        val composeMaterialIconsExtended = "androidx.compose.material:material-icons-extended:$composeVersion"
    }
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